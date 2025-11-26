package com.example.proyectoweb.Modelo;


import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Persona",
        uniqueConstraints = @UniqueConstraint(name="uk_persona_email", columnNames = "email"))
public class Persona {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="name", nullable=false, length=100)
    private String name;

    @Column(name="email", nullable=false, length=100)
    private String email;

    @Column(name="password", nullable=false, length=100)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="organization_id", foreignKey = @ForeignKey(name="fk_persona_org"))
    private Organization organization;

    @Enumerated(EnumType.STRING)
    private Role role;
}
