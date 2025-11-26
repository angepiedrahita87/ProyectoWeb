package com.example.proyectoweb.Servicio;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.proyectoweb.Dto.PersonaDto;
import com.example.proyectoweb.Modelo.Organization;
import com.example.proyectoweb.Modelo.Persona;
import com.example.proyectoweb.Modelo.Role;
import com.example.proyectoweb.Repo.RepoOrganization;
import com.example.proyectoweb.Repo.RepoPersona;
import com.example.proyectoweb.common.DomainExceptions.NotFound;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PersonaService {

    private final RepoPersona repo;
    private final RepoOrganization repoOrg;
    private final ModelMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public PersonaDto crear(PersonaDto dto) {
        Persona e = mapper.map(dto, Persona.class);
        e.setId(null);

        // 🔐 Hashear contraseña
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            e.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        if (dto.getOrganizationId() != null) {
            Organization org = repoOrg.findById(dto.getOrganizationId())
                    .orElseThrow(() -> new NotFound("Organización no encontrada"));
            e.setOrganization(org);
        }

        // 🎭 Asignar rol: si no viene, usamos un default (por ejemplo VIEWER)
        if (dto.getRole() != null) {
            e.setRole(dto.getRole());
        } else if (e.getRole() == null) {
            e.setRole(Role.VIEWER); // default sencillo
        }

        e = repo.save(e);
        return toDto(e);
    }

    @Transactional(readOnly = true)
    public Optional<PersonaDto> obtener(Long id) {
        return repo.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<PersonaDto> listar() {
        List<PersonaDto> out = new ArrayList<>();
        for (Persona e : repo.findAll()) out.add(toDto(e));
        return out;
    }

    @Transactional
    public Optional<PersonaDto> actualizar(Long id, PersonaDto dto) {
        return repo.findById(id).map(existing -> {

            existing.setName(dto.getName());
            existing.setEmail(dto.getEmail());

            // Solo hashear si se envió un password nuevo
            if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
                existing.setPassword(passwordEncoder.encode(dto.getPassword()));
            }

            if (dto.getOrganizationId() != null) {
                Organization org = repoOrg.findById(dto.getOrganizationId())
                        .orElseThrow(() -> new NotFound("Organización no encontrada"));
                existing.setOrganization(org);
            }

            // 🎭 Actualizar rol si se envía uno nuevo en el DTO
            if (dto.getRole() != null) {
                existing.setRole(dto.getRole());
            }

            return toDto(repo.save(existing));
        });
    }

    @Transactional
    public boolean eliminar(Long id) {
        if (!repo.existsById(id)) return false;
        repo.deleteById(id);
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<PersonaDto> obtenerPorEmail(String email) {
        return repo.findByEmailIgnoreCase(email).map(this::toDto);
    }

    private PersonaDto toDto(Persona e) {
        PersonaDto dto = mapper.map(e, PersonaDto.class);
        dto.setOrganizationId(
                e.getOrganization() != null ? e.getOrganization().getId() : null
        );
        // Exponer rol al front
        dto.setRole(e.getRole());
        // Nunca expone el password (ni el hash) al front
        dto.setPassword(null);
        return dto;
    }
}
