package com.example.sgmta.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "test_case")
@Getter
@Setter
@Schema(description = "Represeta um único caso de teste no sistema.")
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "identificador único do TestCase", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Column(name = "test_name", nullable = false)
    @Schema(description = "O nome do TestCase.", example = "shouldAuthenticateUserWithValidCredentials")
    private String testName;

    @Column(name = "flaky", nullable = false)
    @Schema(description = "Indica se o teste é flaky ou não.", example = "false")
    private boolean flaky = false;

    protected TestCase() {}

    public TestCase(String testName, boolean flaky) {
        this.testName = testName;
        this.flaky = flaky;
    }
}
