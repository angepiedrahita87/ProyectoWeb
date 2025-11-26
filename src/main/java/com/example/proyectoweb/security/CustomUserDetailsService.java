package com.example.proyectoweb.security;

import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.proyectoweb.Modelo.Persona;
import com.example.proyectoweb.Modelo.Role;
import com.example.proyectoweb.Repo.RepoPersona;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final RepoPersona repoPersona;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Persona persona = repoPersona.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        // Obtener el rol real desde Persona
        Role role = persona.getRole();
        if (role == null) {
            throw new UsernameNotFoundException("El usuario no tiene un rol asignado");
        }

        GrantedAuthority authority =
                new SimpleGrantedAuthority("ROLE_" + role.name());

        return new User(
                persona.getEmail(),
                persona.getPassword(),
                Collections.singletonList(authority)
        );
    }
}