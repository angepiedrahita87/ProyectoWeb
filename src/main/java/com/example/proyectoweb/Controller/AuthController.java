package com.example.proyectoweb.Controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.proyectoweb.Dto.AuthenticationDto;
import com.example.proyectoweb.Dto.AuthorizedDto;
import com.example.proyectoweb.Dto.PersonaDto;
import com.example.proyectoweb.Servicio.PersonaService;
import com.example.proyectoweb.security.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // por si el front pega desde otro host
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PersonaService personaService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationDto request) {
        try {
            // 1. Autenticar credenciales (email + password)
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // 2. Generar token
            String token = jwtUtil.generateToken(userDetails);

            // 3. Traer info de la persona para el front
            PersonaDto personaDto = personaService.obtenerPorEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            AuthorizedDto response = new AuthorizedDto(token, personaDto);

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(401).body("Credenciales inválidas");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        PersonaDto persona = personaService.obtenerPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return ResponseEntity.ok(persona);
    }

    @PostMapping("/register")
    public ResponseEntity<PersonaDto> register(@RequestBody PersonaDto dto) {
        PersonaDto created = personaService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
