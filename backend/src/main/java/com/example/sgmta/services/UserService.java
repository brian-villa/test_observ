package com.example.sgmta.services;

import com.example.sgmta.dtos.user.UserUpdateDTO;
import com.example.sgmta.entities.User;
import com.example.sgmta.repositories.UserRepository;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Serviço responsável pela lógica de negócio de utilizadores.
 */
@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Lista todos os utilizadores (Útil para perfis de Administrador).
     */
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Procura um utilizador pelo seu ID (UUID).
     */
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado."));
    }

    /**
     * Atualiza os dados permitidos do utilizador.
     * Atualização de Email ou Password ver AuthService
     */
    public User update(UUID id, UserUpdateDTO data) {
        User user = findById(id);
        if (data.name() != null) user.setName(data.name());
        return userRepository.save(user);
    }

    /**
     * Remove um utilizador do sistema.
     */
    public void delete(UUID id) {
        User user = findById(id);
        userRepository.delete(user);
    }

}
