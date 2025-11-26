package com.example.proyectoweb.Servicio;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.proyectoweb.Dto.ProcesoDto;
import com.example.proyectoweb.Modelo.Actividad;
import com.example.proyectoweb.Modelo.Arch;
import com.example.proyectoweb.Modelo.Gateway;
import com.example.proyectoweb.Modelo.Organization;
import com.example.proyectoweb.Modelo.Persona;
import com.example.proyectoweb.Modelo.Proceso;
import com.example.proyectoweb.Modelo.ProcessHistory;
import com.example.proyectoweb.Modelo.Role;
import com.example.proyectoweb.Repo.RepoActividad;
import com.example.proyectoweb.Repo.RepoArch;
import com.example.proyectoweb.Repo.RepoGateway;
import com.example.proyectoweb.Repo.RepoOrganization;
import com.example.proyectoweb.Repo.RepoPersona;
import com.example.proyectoweb.Repo.RepoProceso;
import com.example.proyectoweb.Repo.RepoProcessHistory;
import com.example.proyectoweb.common.DomainExceptions.NotFound;
import com.example.proyectoweb.common.ProcessStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProcesoService {

    private final RepoProceso repo;
    private final RepoOrganization repoOrg;
    private final RepoActividad repoActividad;
    private final RepoArch repoArch;
    private final RepoGateway repoGateway;
    private final RepoProcessHistory repoHistory;
    private final RepoPersona repoPersona;   // 👈 nuevo

    // ================== HELPERS ==================

    private Persona getActor(String actorEmail) {
        return repoPersona.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new NotFound("Actor no encontrado"));
    }

    private void checkSameOrg(Proceso p, Persona actor) {
        Long orgProceso = p.getOrganization() != null ? p.getOrganization().getId() : null;
        Long orgActor = actor.getOrganization() != null ? actor.getOrganization().getId() : null;

        if (orgProceso == null || orgActor == null || !orgProceso.equals(orgActor)) {
            throw new RuntimeException("No autorizado para operar sobre procesos de otra organización");
        }
    }

    // ================== CRUD ==================

    @Transactional
    public ProcesoDto crear(ProcesoDto dto, String actorEmail) {
        Persona actor = getActor(actorEmail);

        Proceso p = new Proceso();
        p.setId(null);
        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setCategory(dto.getCategory());
        p.setStatus(dto.getStatus() != null ? dto.getStatus() : ProcessStatus.DRAFT);

        // 👇 La organización se toma SIEMPRE del actor (no del DTO)
        Organization org = actor.getOrganization();
        if (org == null) {
            throw new RuntimeException("El usuario no tiene organización asociada");
        }
        p.setOrganization(org);

        if (dto.getActivityIds() != null) p.setActivities(resolveActivities(dto.getActivityIds()));
        if (dto.getArchIds() != null) p.setArchs(resolveArches(dto.getArchIds()));
        if (dto.getGatewayIds() != null) p.setGateways(resolveGateways(dto.getGatewayIds()));

        p = repo.save(p);
        addHistory(p, actorEmail, "Creación");

        return toDto(p);
    }

    @Transactional(readOnly = true)
    public Optional<ProcesoDto> obtener(Long id, String actorEmail) {
        Persona actor = getActor(actorEmail);
        return repo.findById(id)
                .filter(p -> {
                    // solo devolver si pertenece a la misma org
                    Organization org = p.getOrganization();
                    return org != null && actor.getOrganization() != null
                            && org.getId().equals(actor.getOrganization().getId());
                })
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<ProcesoDto> listar(String actorEmail, ProcessStatus status) {
        Persona actor = getActor(actorEmail);
        Long orgId = actor.getOrganization() != null ? actor.getOrganization().getId() : null;
        if (orgId == null) {
            return new ArrayList<>();
        }

        List<Proceso> base = repo.findAllByOrganization_Id(orgId);

        if (status != null) {
            base = base.stream()
                    .filter(p -> p.getStatus() == status)
                    .collect(Collectors.toList());
        }
        return base.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public Optional<ProcesoDto> actualizar(Long id, ProcesoDto dto, String actorEmail) {
        Persona actor = getActor(actorEmail);

        return repo.findById(id).map(existing -> {

            // valida organización
            checkSameOrg(existing, actor);

            if (dto.getName() != null) existing.setName(dto.getName());
            if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
            if (dto.getCategory() != null) existing.setCategory(dto.getCategory());
            if (dto.getStatus() != null) existing.setStatus(dto.getStatus());

            // 👇 Podrías permitir cambiar de organización solo a SUPERADMIN, de momento no:
            // if (dto.getOrganizationId()!=null) ...

            if (dto.getActivityIds() != null) existing.setActivities(resolveActivities(dto.getActivityIds()));
            if (dto.getArchIds() != null) existing.setArchs(resolveArches(dto.getArchIds()));
            if (dto.getGatewayIds() != null) existing.setGateways(resolveGateways(dto.getGatewayIds()));

            Proceso saved = repo.save(existing);
            addHistory(saved, actorEmail, "Actualización");
            return toDto(saved);
        });
    }

    @Transactional
    public boolean eliminar(Long id, boolean hardDelete, String actorEmail) {
        Persona actor = getActor(actorEmail);

        return repo.findById(id).map(p -> {

            // validar organización
            checkSameOrg(p, actor);

            if (hardDelete) {
                // Solo ADMIN puede hard delete
                if (actor.getRole() != Role.ADMIN) {
                    throw new RuntimeException("Solo un ADMIN puede eliminar definitivamente el proceso");
                }

                // 1️⃣ borrar historial asociado al proceso
                var history = repoHistory.findAllByProceso_IdOrderByCreatedAtDesc(p.getId());
                repoHistory.deleteAll(history);

                // 2️⃣ borrar el proceso
                repo.delete(p);

            } else {
                // Soft delete: marcar INACTIVE y dejar historial vivo
                if (p.getStatus() == ProcessStatus.PUBLISHED) {
                    p.setStatus(ProcessStatus.INACTIVE);
                    repo.save(p);
                    addHistory(p, actorEmail, "Soft delete (INACTIVE)");
                } else {
                    // si quieres, podrías simplemente borrarlo si está en DRAFT
                    // repo.delete(p);
                    p.setStatus(ProcessStatus.INACTIVE);
                    repo.save(p);
                    addHistory(p, actorEmail, "Soft delete (INACTIVE)");
                }
            }

            return true;
        }).orElse(false);
    }

    @Transactional(readOnly = true)
    public List<ProcessHistory> historial(Long procesoId, String actorEmail) {
        Persona actor = getActor(actorEmail);
        Proceso p = repo.findById(procesoId)
                .orElseThrow(() -> new NotFound("Proceso no encontrado"));
        checkSameOrg(p, actor);

        return repoHistory.findAllByProceso_IdOrderByCreatedAtDesc(procesoId);
    }

    // ================== HELPERS RELACIONADOS ==================

    private List<Actividad> resolveActivities(List<Long> ids) {
        if (ids == null) return new ArrayList<>();
        List<Actividad> out = new ArrayList<>(ids.size());
        for (Long id : ids) {
            out.add(repoActividad.findById(id)
                    .orElseThrow(() -> new NotFound("Actividad no existe: " + id)));
        }
        return out;
    }

    private List<Arch> resolveArches(List<Long> ids) {
        if (ids == null) return new ArrayList<>();
        List<Arch> out = new ArrayList<>(ids.size());
        for (Long id : ids) {
            out.add(repoArch.findById(id)
                    .orElseThrow(() -> new NotFound("Arch no existe: " + id)));
        }
        return out;
    }

    private List<Gateway> resolveGateways(List<Long> ids) {
        if (ids == null) return new ArrayList<>();
        List<Gateway> out = new ArrayList<>(ids.size());
        for (Long id : ids) {
            out.add(repoGateway.findById(id)
                    .orElseThrow(() -> new NotFound("Gateway no existe: " + id)));
        }
        return out;
    }

    private void addHistory(Proceso p, String actorEmail, String reason) {
        ProcessHistory h = new ProcessHistory();
        h.setProceso(p);
        h.setChangedBy(actorEmail != null ? actorEmail : reason);
        h.setStatus(p.getStatus());
        h.setDescriptionSnapshot(p.getDescription());
        repoHistory.save(h);
    }

    private ProcesoDto toDto(Proceso p) {
        ProcesoDto dto = new ProcesoDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setCategory(p.getCategory());
        dto.setStatus(p.getStatus());
        dto.setOrganizationId(p.getOrganization() != null ? p.getOrganization().getId() : null);
        dto.setActivityIds(p.getActivities().stream().map(Actividad::getId).collect(Collectors.toList()));
        dto.setArchIds(p.getArchs().stream().map(Arch::getId).collect(Collectors.toList()));
        dto.setGatewayIds(p.getGateways().stream().map(Gateway::getId).collect(Collectors.toList()));
        return dto;
    }
}
