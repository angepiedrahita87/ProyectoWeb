package com.example.proyectoweb.Dto;

import com.example.proyectoweb.Modelo.Role;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonaDto {
    private Long id;
    private String name;
    private String email;
    private String password;
    private Long organizationId;
    private Role role;
}
