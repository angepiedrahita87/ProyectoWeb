package com.example.proyectoweb.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.proyectoweb.Dto.ActivityRoleLinkDto;
import com.example.proyectoweb.Dto.ProcessRoleDto;
import com.example.proyectoweb.Servicio.ProcessRoleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/process-roles")
@CrossOrigin(origins = "http://localhost:4200")
public class ProcessRoleController {

    private final ProcessRoleService service;

    // 🔹 Listar roles de la organización del usuario
    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public List<ProcessRoleDto> list(@AuthenticationPrincipal UserDetails user) {
        return service.listar(user.getUsername());
    }

    // 🔹 Crear rol (ADMIN o EDITOR)
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ResponseEntity<ProcessRoleDto> create(@RequestBody ProcessRoleDto dto,
                                                 @AuthenticationPrincipal UserDetails user) {
        ProcessRoleDto created = service.crear(dto, user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // 🔹 Actualizar rol (ADMIN o EDITOR)
    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ResponseEntity<ProcessRoleDto> update(@PathVariable Long id,
                                                 @RequestBody ProcessRoleDto dto,
                                                 @AuthenticationPrincipal UserDetails user) {
        return service.actualizar(id, dto, user.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 🔹 Eliminar rol (solo ADMIN)
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails user,
                                       @RequestHeader(value = "X-Confirm-Delete", required = false) String confirm) {
        // conservamos tu confirmación por header
        if (!"true".equalsIgnoreCase(confirm)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).build();
        }

        boolean ok = service.eliminar(id, user.getUsername());
        return ok ? ResponseEntity.noContent().build()
                  : ResponseEntity.notFound().build();
    }

    // 🔹 Asignar rol a actividad (ADMIN o EDITOR)
    @PostMapping("/assign-activity")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ActivityRoleLinkDto assign(@RequestParam Long actividadId,
                                      @RequestParam Long roleId,
                                      @AuthenticationPrincipal UserDetails user) {
        return service.asignarRolActividad(actividadId, roleId, user.getUsername());
    }

    // 🔹 Ver dónde se usa un rol (cualquier user de la org)
    @GetMapping("/{id}/usage")
    @PreAuthorize("isAuthenticated()")
    public Map<String, List<Long>> usage(@PathVariable Long id,
                                         @AuthenticationPrincipal UserDetails user) {
        return service.dondeSeUsa(id, user.getUsername());
    }
}
