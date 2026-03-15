package com.example.sgmta.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "project")
@Getter
@Setter
@Schema(description = "Representação de um projeto de testes, a sua equipa e as credenciais de integração")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "Identificador único do projeto", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Column(nullable = false)
    @Schema(description = "Nome do projeto a ser monitorizado", example = "Plataforma de testes")
    private String name;

    @Column(length = 500)
    @Schema(description = "Descrição ou finalidade do projeto")
    private String description;

    /**
     * API Key para integração de sistemas externos.
     */
    @Column(name = "project_token", nullable = false, unique = true)
    @Schema(description = "Token de acesso estático para integração Machine-to-Machine", example = "sgmta_9f86d081884c7d659a2feaa0c55ad015a")
    private String projectToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "Data e hora de registo do projeto")
    private LocalDateTime createdAt;

    /**
     *
     * Um projeto contém uma lista de utilizadores (equipa).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "project_users",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Schema(description = "Lista de utilizadores que colaboram neste projeto")
    private Set<User> users = new HashSet<>();

    protected Project() {}

    public Project(String name, String description, String projectToken) {
        this.name = name;
        this.description = description;
        this.projectToken = projectToken;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Métodos utilitários para gestão segura da coleção de utilizadores.
     */
    public void addUser(User user) {
        this.users.add(user);
    }

    public void removeUser(User user) {
        this.users.remove(user);
    }

}
