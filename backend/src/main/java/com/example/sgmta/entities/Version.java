package com.example.sgmta.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "version")
@Getter
@Setter
@Schema(description = "Representa a versão do software")
public class Version {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "Identificador único da versão", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Column(name = "version_name", nullable = false)
    @Schema(description = "O nome da versão do software", example = "v1.0.0")
    private String versionName;

    protected Version() {}

    public Version(String versionName) {
        this.versionName = versionName;
    }

}
