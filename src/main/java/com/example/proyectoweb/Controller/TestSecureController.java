package com.example.proyectoweb.Controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class TestSecureController {

    @GetMapping("/api/secure/hello")
    public String helloSecure() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName(); // viene del UserDetails (tu email)

        return "Hola " + email + ", este endpoint está protegido 😎";
    }
}