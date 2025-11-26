package com.example.proyectoweb.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.proyectoweb.Dto.ArchDto;
import com.example.proyectoweb.Servicio.ArchService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/archs")
@CrossOrigin(origins = "http://localhost:4200")
public class ArchController {

    private final ArchService service;

    // 🔹 Listar arcos (cualquier user logueado)
    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public List<ArchDto> list() {
        return service.listar();
    }

    // 🔹 Obtener arco por id (cualquier user logueado)
    @GetMapping("/get/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ArchDto> get(@PathVariable Long id) {
        return service.obtener(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 🔹 Crear arco (ADMIN o EDITOR)
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ResponseEntity<ArchDto> create(@RequestBody ArchDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    // 🔹 Actualizar arco (ADMIN o EDITOR)
    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ResponseEntity<ArchDto> update(@PathVariable Long id, @RequestBody ArchDto dto) {
        return service.actualizar(id, dto).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 🔹 Eliminar arco (solo ADMIN, y con confirm header como ya tenías)
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @RequestHeader(value="X-Confirm-Delete", required=false) String confirm) {
        if (!"true".equalsIgnoreCase(confirm)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).build();
        }
        return service.eliminar(id) ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
