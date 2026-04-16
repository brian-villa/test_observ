package com.example.sgmta.security;

import com.example.sgmta.repositories.UserRepository;
import com.example.sgmta.services.TokenService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de Segurança para interceptação de pedidos JWT (RF.02).
 * Verifica a validade do token em cada pedido e autentica o utilizador no contexto do Spring.
 */
@Component
@Schema(description = "Interceptador de segurança que valida o Bearer Token em cada pedido")
public class SecurityFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final UserRepository userRepository;

    public SecurityFilter(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = this.recoverToken(request);

        if (token != null) {
            var email = tokenService.validateToken(token);

            // Se o token for válido e o email existir, carregamos o utilizador
            if (!email.isEmpty()) {
                UserDetails user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Utilizador não encontrado no filtro"));

                var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

                // contexto de segurança
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // Continua o fluxo para o Controller
        filterChain.doFilter(request, response);
    }

    /**
     * Extrai o token do cabeçalho 'Authorization: Bearer <token>'.
     */
    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }


}
