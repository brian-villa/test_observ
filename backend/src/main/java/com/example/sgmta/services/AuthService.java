package com.example.sgmta.services;

import com.example.sgmta.dtos.auth.LoginDTO;
import com.example.sgmta.dtos.auth.RegisterDTO;
import com.example.sgmta.entities.User;
import com.example.sgmta.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Serviço de Autenticação.
 * Lida com o registo de novos utilizadores e futura emissão de Tokens JWT.
 */
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /**
     * Regista um utilizador no SGMTA.
     * Cifra a password e valida a unicidade do email.
     */
    @Transactional
    public User register(RegisterDTO data) {
        if (userRepository.findByEmail(data.email()).isPresent()) {
            throw new RuntimeException("Email já registado no sistema.");
        }

        String hash = passwordEncoder.encode(data.password());
        User newUser = new User(data.name(), data.email(), hash);
        return userRepository.save(newUser);
    }

    /**
     * Valida as credenciais do utilizador.
     */
    public User login(LoginDTO data) {
        User user = userRepository.findByEmail(data.email())
                .orElseThrow(() -> new RuntimeException("Credenciais inválidas."));

        if (!passwordEncoder.matches(data.password(), user.getPassword())) {
            throw new RuntimeException("Credenciais inválidas.");
        }

        return user;
    }

    /**
     * Altera a password de forma segura, exigindo a validação da atual.
     */
    @Transactional
    public void updatePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado."));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("A password atual está incorreta.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Altera o email verificando a disponibilidade do novo.
     */
    @Transactional
    public void updateEmail(String currentEmail, String newEmail) {
        if (userRepository.findByEmail(newEmail).isPresent()) {
            throw new RuntimeException("O novo email já está em uso.");
        }

        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado."));

        user.setEmail(newEmail);
        userRepository.save(user);
    }
}
