package com.example.sgmta.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Entidade que representa um Utilizador no sistema
 * Mapeada para a tabela 'users' para evitar conflitos com palavras reservadas (ex: user) do PostgreSQL.
 */

@Entity
@Table(name = "users")
@Getter
@Setter
@Schema(description = "Representação de um utilizador na base de dados")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "Identificador único (UUID) gerado automaticamente")
    private UUID id;

    @Column(nullable = false)
    @Schema(example = "XPTO", description = "Nome do utilizador")
    private String name;

    @Column(nullable = false, unique = true)
    @Schema(example = "xpto@example.com")
    private String email;

    @Column(nullable = false)
    @Schema(description = "Hash da password cifrado utilizando o algoritmo do BCrypt")
    private String password;

    protected User () {}

    /**
     * Construtor principal para criação de novos utilizadores via UserService.
     */
    public User (String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    /**
     * Retorna as permissões do utilizador.
     * Por agora, atribuímos uma role padrão
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    /**
     * O Spring Security usa o username para identificar o utilizador.
     * No SGMTA, usamos o email como identificador único.
     */
    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
