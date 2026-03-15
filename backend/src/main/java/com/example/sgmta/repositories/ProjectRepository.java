package com.example.sgmta.repositories;

import com.example.sgmta.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para acesso aos dados da entidade Project.
 * O Spring Data JPA implementa esta interface automaticamente em tempo de execução.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    /**
     * Procura um projeto utilizando a API Key.
     * Permite identificar de que projeto
     * vêm os testes sem precisar do ID .
     */
    Optional<Project> findByProjectToken(String projectToken);

    /**
     * Verifica se já existe um projeto com o mesmo nome.
     */
    boolean existsByName(String name);
}
