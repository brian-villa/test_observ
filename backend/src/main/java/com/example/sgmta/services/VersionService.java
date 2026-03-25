package com.example.sgmta.services;

import com.example.sgmta.entities.Version;
import com.example.sgmta.repositories.VersionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço responsável pela gestão das versões de software.
 * Atua como um dicionário para evitar a duplicação de versões durante a ingestão de testes.
 */
@Service
public class VersionService {
    private final VersionRepository versionRepository;

    public VersionService(VersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    /**
     * Lógica de "Find or Create".
     * Se a versão enviada pela pipeline já existir, devolve a existente.
     * Se for uma versão nova, grava na base de dados e devolve.
     *
     * @param versionName O nome da versão (ex: "v1.0.5")
     * @return Entidade Version persistida.
     */
    @Transactional
    public Version findOrCreate(String versionName) {
        return versionRepository.findByVersionName(versionName)
                .orElseGet(() -> versionRepository.save(new Version(versionName)));
    }

    /**
     * Recupera todas as versões.
     */
    public List<Version> findAll() {
        return versionRepository.findAll();
    }

}
