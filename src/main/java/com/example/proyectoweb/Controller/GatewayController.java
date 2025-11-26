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

import com.example.proyectoweb.Dto.GatewayDto;
import com.example.proyectoweb.Servicio.GatewayService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gateways")
@CrossOrigin(origins = "http://localhost:4200")
public class GatewayController {

    private final GatewayService service;

    // Listar gateways (cualquier user logueado)
    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public List<GatewayDto> list() {
        return service.listar();
    }

    // Obtener gateway por id (cualquier user logueado)
    @GetMapping("/get/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GatewayDto> get(@PathVariable Long id) {
        return service.obtener(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Crear gateway (ADMIN o EDITOR)
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ResponseEntity<GatewayDto> create(@RequestBody GatewayDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    // Actualizar gateway (ADMIN o EDITOR)
    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ResponseEntity<GatewayDto> update(@PathVariable Long id, @RequestBody GatewayDto dto) {
        return service.actualizar(id, dto).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Eliminar gateway (solo ADMIN, con confirm como ya lo tenías)
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