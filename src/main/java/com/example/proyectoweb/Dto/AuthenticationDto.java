package com.example.proyectoweb.Dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class AuthenticationDto {
    private String email;
    private String password;
}