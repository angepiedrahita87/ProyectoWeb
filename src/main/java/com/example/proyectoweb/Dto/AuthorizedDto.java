package com.example.proyectoweb.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
@AllArgsConstructor
public class AuthorizedDto {
    private String token;
    private PersonaDto persona; 
    private String roleName;
}