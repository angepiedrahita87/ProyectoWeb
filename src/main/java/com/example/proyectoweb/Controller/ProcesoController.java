package com.example.proyectoweb.Controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.proyectoweb.Dto.ProcesoDto;
import com.example.proyectoweb.Modelo.ProcessHistory;
import com.example.proyectoweb.Servicio.ProcesoService;
import com.example.proyectoweb.common.ProcessStatus;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/processes")
@CrossOrigin(origins = "http://localhost:4200")
public class ProcesoController {

    private final ProcesoService service;

    // 🔹 Listar procesos de la organización del usuario logueado
    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public List<ProcesoDto> list(@AuthenticationPrincipal UserDetails user,
                                 @RequestParam(required = false) ProcessStatus status) {
        return service.listar(user.getUsername(), status);
    }

    // Obtener detalle de un proceso (solo si es de su org)
    @GetMapping("/get/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcesoDto> get(@PathVariable Long id,
                                          @AuthenticationPrincipal UserDetails user) {
        return service.obtener(id, user.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Crear proceso (ADMIN o EDITOR)
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ResponseEntity<ProcesoDto> create(@RequestBody ProcesoDto dto,
                                             @AuthenticationPrincipal UserDetails user) {
        ProcesoDto created = service.crear(dto, user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Actualizar proceso (ADMIN o EDITOR)
    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ResponseEntity<ProcesoDto> update(@PathVariable Long id,
                                             @RequestBody ProcesoDto dto,
                                             @AuthenticationPrincipal UserDetails user) {
        return service.actualizar(id, dto, user.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Eliminar proceso
    //    - Soft delete lo puede intentar ADMIN/EDITOR
    //    - Hard delete lo bloquea el service si no es ADMIN
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @RequestParam(defaultValue = "false") boolean hardDelete,
                                       @AuthenticationPrincipal UserDetails user) {
        boolean ok = service.eliminar(id, hardDelete, user.getUsername());
        return ok ? ResponseEntity.noContent().build()
                  : ResponseEntity.notFound().build();
    }

    // Historial de cambios del proceso (solo si es de su org)
    @GetMapping("/{id}/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProcessHistory>> history(@PathVariable Long id,
                                                        @AuthenticationPrincipal UserDetails user) {
        List<ProcessHistory> history = service.historial(id, user.getUsername());
        return ResponseEntity.ok(history);
    }
}
