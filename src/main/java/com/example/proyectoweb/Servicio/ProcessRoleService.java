package com.example.proyectoweb.Servicio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.proyectoweb.Dto.ActivityRoleLinkDto;
import com.example.proyectoweb.Dto.ProcessRoleDto;
import com.example.proyectoweb.Modelo.Actividad;
import com.example.proyectoweb.Modelo.ActivityRoleLink;
import com.example.proyectoweb.Modelo.Organization;
import com.example.proyectoweb.Modelo.Persona;
import com.example.proyectoweb.Modelo.Proceso;
import com.example.proyectoweb.Modelo.ProcessRole;
import com.example.proyectoweb.Repo.RepoActividad;
import com.example.proyectoweb.Repo.RepoActivityRoleLink;
import com.example.proyectoweb.Repo.RepoOrganization;
import com.example.proyectoweb.Repo.RepoPersona;
import com.example.proyectoweb.Repo.RepoProceso;
import com.example.proyectoweb.Repo.RepoProcessRole;
import com.example.proyectoweb.common.DomainExceptions.Conflict;
import com.example.proyectoweb.common.DomainExceptions.NotFound;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProcessRoleService {

    private final RepoProcessRole repoRole;
    private final RepoOrganization repoOrg;
    private final RepoActividad repoActividad;
    private final RepoActivityRoleLink repoLink;
    private final RepoProceso repoProceso;
    private final RepoPersona repoPersona;   // 👈 nuevo

    // =============== HELPERS ===============

    private Persona getActor(String actorEmail) {
        return repoPersona.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new NotFound("Actor no encontrado"));
    }

    private Long getActorOrgId(Persona actor) {
        if (actor.getOrganization() == null) {
            throw new RuntimeException("El usuario no tiene organización asociada");
        }
        return actor.getOrganization().getId();
    }

    private void checkSameOrg(ProcessRole role, Persona actor) {
        Long orgRole = role.getOrganization() != null ? role.getOrganization().getId() : null;
        Long orgActor = getActorOrgId(actor);
        if (orgRole == null || !orgRole.equals(orgActor)) {
            throw new RuntimeException("No autorizado para operar roles de otra organización");
        }
    }

    // =============== CRUD ROLES ===============

    @Transactional
    public ProcessRoleDto crear(ProcessRoleDto dto, String actorEmail) {
        Persona actor = getActor(actorEmail);
        Long orgId = getActorOrgId(actor);

        ProcessRole r = new ProcessRole();
        r.setId(null);
        r.setName(dto.getName());

        // 👇 La org SIEMPRE es la del actor
        Organization org = repoOrg.findById(orgId)
                .orElseThrow(() -> new NotFound("Organización no encontrada"));
        r.setOrganization(org);

        r = repoRole.save(r);
        return new ProcessRoleDto(r.getId(), r.getName(), org.getId());
    }

    @Transactional(readOnly = true)
    public List<ProcessRoleDto> listar(String actorEmail) {
        Persona actor = getActor(actorEmail);
        Long orgId = getActorOrgId(actor);

        return repoRole.findAllByOrganization_Id(orgId)
                .stream()
                .map(r -> new ProcessRoleDto(
                        r.getId(),
                        r.getName(),
                        r.getOrganization() != null ? r.getOrganization().getId() : null
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<ProcessRoleDto> actualizar(Long id, ProcessRoleDto dto, String actorEmail) {
        Persona actor = getActor(actorEmail);

        return repoRole.findById(id).map(existing -> {
            // validar que el rol pertenece a la misma org
            checkSameOrg(existing, actor);

            if (dto.getName() != null) {
                existing.setName(dto.getName());
            }

            // 👇 No permitimos cambiar de organización desde el DTO
            // si quisieras permitirlo, acá se validaría súper admin, etc.

            ProcessRole saved = repoRole.save(existing);
            return new ProcessRoleDto(
                    saved.getId(),
                    saved.getName(),
                    saved.getOrganization() != null ? saved.getOrganization().getId() : null
            );
        });
    }

    @Transactional
    public boolean eliminar(Long id, String actorEmail) {
        Persona actor = getActor(actorEmail);
        Long orgIdActor = getActorOrgId(actor);

        ProcessRole role = repoRole.findById(id)
                .orElseThrow(() -> new NotFound("Rol no encontrado"));

        // validar organización
        if (role.getOrganization() == null ||
                !role.getOrganization().getId().equals(orgIdActor)) {
            throw new RuntimeException("No autorizado para eliminar roles de otra organización");
        }

        if (repoLink.existsByRole_Id(id)) {
            throw new Conflict("No se puede eliminar: el rol está en uso por una o más actividades");
        }

        repoRole.deleteById(id);
        return true;
    }

    // =============== ASIGNACIÓN A ACTIVIDADES ===============

    @Transactional
    public ActivityRoleLinkDto asignarRolActividad(Long actividadId, Long roleId, String actorEmail) {
        Persona actor = getActor(actorEmail);
        Long orgIdActor = getActorOrgId(actor);

        Actividad act = repoActividad.findById(actividadId)
                .orElseThrow(() -> new NotFound("Actividad no encontrada"));
        ProcessRole role = repoRole.findById(roleId)
                .orElseThrow(() -> new NotFound("Rol no encontrado"));

        // validar que el rol pertenece a la misma org del actor
        if (role.getOrganization() == null ||
                !role.getOrganization().getId().equals(orgIdActor)) {
            throw new RuntimeException("No autorizado para usar roles de otra organización");
        }

        ActivityRoleLink link = new ActivityRoleLink(null, act, role);
        link = repoLink.save(link);
        return new ActivityRoleLinkDto(link.getId(), act.getId(), role.getId());
    }

    // =============== USO DEL ROL ===============

    @Transactional(readOnly = true)
    public Map<String, List<Long>> dondeSeUsa(Long roleId, String actorEmail) {
        Persona actor = getActor(actorEmail);
        Long orgIdActor = getActorOrgId(actor);

        ProcessRole role = repoRole.findById(roleId)
                .orElseThrow(() -> new NotFound("Rol no encontrado"));

        // validar que el rol es de su org
        if (role.getOrganization() == null ||
                !role.getOrganization().getId().equals(orgIdActor)) {
            throw new RuntimeException("No autorizado para consultar roles de otra organización");
        }

        List<ActivityRoleLink> links = repoLink.findAllByRole_Id(roleId);
        List<Long> actividadIds = links.stream()
                .map(l -> l.getActividad().getId())
                .collect(Collectors.toList());

        Set<Long> procesoIds = new HashSet<>();
        for (Proceso p : repoProceso.findAll()) {
            // Solo procesos de la misma organización
            if (p.getOrganization() == null ||
                    !p.getOrganization().getId().equals(orgIdActor)) {
                continue;
            }
            boolean used = p.getActivities().stream()
                    .anyMatch(a -> actividadIds.contains(a.getId()));
            if (used) procesoIds.add(p.getId());
        }

        Map<String, List<Long>> out = new HashMap<>();
        out.put("actividades", actividadIds);
        out.put("procesos", new ArrayList<>(procesoIds));
        return out;
    }
}
