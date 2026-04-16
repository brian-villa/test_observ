package com.example.sgmta.services;

import com.example.sgmta.dtos.project.ProjectCreateDTO;
import com.example.sgmta.dtos.project.ProjectUpdateDTO;
import com.example.sgmta.entities.Project;
import com.example.sgmta.entities.User;
import com.example.sgmta.repositories.ProjectRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;

/**
 * Serviço responsável pela lógica de negócio da gestão de Projetos.
 * Orquestra a criação, validação e geração de credenciais de integração.
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /**
     * Cria um novo projeto, gera a API Key estática e associa o utilizador autenticado à equipa.
     * * @param data Dados enviados pelo frontend (nome e descrição).
     * @param creator O utilizador autenticado que está a criar o projeto.
     * @return O projeto persistido na base de dados.
     */
    @Transactional
    public Project create(ProjectCreateDTO data, User creator) {
        if (projectRepository.existsByName(data.name())) {
            throw new RuntimeException("Já existe um projeto com este nome no sistema.");
        }

        String projectToken = generateTokenFromName(data.name());
        Project newProject = new Project(data.name(), data.description(), projectToken);
        newProject.addUser(creator);



        return projectRepository.save(newProject);
    }

    /**
     * Atualiza os dados básicos do projeto Nome e Descrição
     */
    @Transactional
    public Project update(UUID id, ProjectUpdateDTO data) {
        Project project = findById(id);

        // Se o nome for alterado, validamos se não choca com outro existente
        if (data.name() != null && !data.name().equals(project.getName())) {
            if (projectRepository.existsByName(data.name())) {
                throw new RuntimeException("O novo nome já está em uso por outro projeto.");
            }
            project.setName(data.name());
        }

        if (data.description() != null) {
            project.setDescription(data.description());
        }

        if (data.flakyThreshold() != null) {
            project.setFlakyThreshold(data.flakyThreshold());
        }

        return projectRepository.save(project);
    }

    /**
     * Elimina um projeto permanentemente
     */
    @Transactional
    public void delete(UUID id) {
        Project project = findById(id);
        projectRepository.delete(project);
    }

    /**
     * Roda a API Key de um projeto.
     * Invalida o acesso de qualquer pipeline que esteja a usar a chave antiga.
     */
    @Transactional
    public Project rotateToken(UUID id) {
        Project project = findById(id);

        // Chamamos o nosso método auxiliar
        String newToken = generateTokenFromName(project.getName());
        project.setProjectToken(newToken);

        return projectRepository.save(project);
    }

    /**
     * Recupera um projeto pelo seu ID interno.
     * * @param id UUID do projeto.
     * @return A entidade Project encontrada.
     * @throws RuntimeException se o projeto não existir.
     */
    public Project findById(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado."));
    }

    /**
     * Recupera um projeto pela sua API Key.
     * * @param token A chave estática gerada.
     * @return A entidade Project encontrada.
     * @throws RuntimeException se a chave for inválida.
     */
    public Project findByToken(String token) {
        return projectRepository.findByProjectToken(token)
                .orElseThrow(() -> new RuntimeException("API Key inválida ou projeto inexistente."));
    }

    /**
     * Gera um token seguro usando o nome do projeto normalizado como prefixo.
     * Exemplo: "Gestão de Testes" -> "gestaodetestes-9f86d081884"
     */
    private String generateTokenFromName(String projectName) {
        //Remove acentos
        String normalized = Normalizer.normalize(projectName, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        //Remove tudo o que não for letra ou número e passa para minúsculas
        String safePrefix = normalized.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        String rawUuid = UUID.randomUUID().toString().replace("-", "");

        return safePrefix + "-" + rawUuid;
    }

    /**
     * Devolve a lista de projetos associados a um utilizador
     */
    public List<Project> findAllByUsers(User user) {
        return projectRepository.findByUsersContaining(user);
    }
}
