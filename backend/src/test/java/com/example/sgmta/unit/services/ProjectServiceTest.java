package com.example.sgmta.unit.services;

import com.example.sgmta.dtos.project.ProjectCreateDTO;
import com.example.sgmta.entities.Project;
import com.example.sgmta.entities.User;
import com.example.sgmta.repositories.ProjectRepository;
import com.example.sgmta.services.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void shouldCreateProjectSuccessfully() {
        // Arrange
        ProjectCreateDTO dto = new ProjectCreateDTO("Plataforma X", "Descrição da plataforma X");
        User mockUser = mock(User.class);
        
        when(projectRepository.existsByName(dto.name())).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Project result = projectService.create(dto, mockUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Plataforma X");
        assertThat(result.getProjectToken()).startsWith("plataformax-");
        
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void shouldThrowExceptionWhenProjectNameAlreadyExists() {
        // Arrange
        ProjectCreateDTO dto = new ProjectCreateDTO("Plataforma Existente", "Descrição");
        User mockUser = mock(User.class);
        
        when(projectRepository.existsByName(dto.name())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> projectService.create(dto, mockUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Já existe um projeto com este nome no sistema.");
        
        verify(projectRepository, never()).save(any(Project.class));
    }
}
