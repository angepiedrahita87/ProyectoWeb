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

        // Por ahora todos ROLE_USER. Si algún día meten roles, se cambia acá.
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");

        return new User(
                persona.getEmail(),
                persona.getPassword(),
                Collections.singletonList(authority)
        );
    }
}