package com.example.sgmta.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.example.sgmta.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Serviço responsável pela geração e validação de Tokens JWT.
 */
@Service
public class TokenService {
    @Value("${api.security.jwt.secret}")
    private String secret;

    /**
     * Gera um Token JWT para um utilizador autenticado.
     * @param user O utilizador que realizou login.
     * @return String contendo o token assinado.
     */
    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("sgmta-api")
                    .withSubject(user.getEmail())
                    .withExpiresAt(genExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token", exception);
        }
    }

    /**
     * Valida um token e extrai o email do utilizador.
     * @param token O token recebido no header Authorization.
     * @return O email do utilizador ou string vazia se inválido.
     */
    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("sgmta-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            return "";
        }
    }

    /**
     * Gera a data de expiração do token no formato Instant (UTC).
     * O uso de Instant é necessário para garantir que a expiração seja
     * independente do fuso horário do servidor ou do cliente.
     * * @return Instant representando o momento exato da expiração.
     */
    private Instant genExpirationDate() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-01:00"));
    }
}
