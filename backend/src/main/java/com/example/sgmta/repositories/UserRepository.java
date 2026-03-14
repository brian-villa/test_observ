package com.example.sgmta.repositories;

import com.example.sgmta.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * Repositório para operações de base de dados na entidade User.
 * Fornece métodos automáticos ou não para CRUD.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Procura um utilizador pelo seu endereço de email.
     * Utilizado para validar se o email já existe e para o processo de Login.
     * * @param email O email a pesquisar.
     * @return Um Optional contendo o utilizador se encontrado, ou vazio caso contrário.
     */
    Optional<User> findByEmail(String email);
}
