package com.example.sgmta.unit.services;

import com.example.sgmta.dtos.auth.LoginDTO;
import com.example.sgmta.dtos.auth.RegisterDTO;
import com.example.sgmta.entities.User;
import com.example.sgmta.repositories.UserRepository;
import com.example.sgmta.services.AuthService;
import com.example.sgmta.services.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRegisterUserSuccessfully() {
        // Arrange
        RegisterDTO dto = new RegisterDTO("João QA", "joao.qa@test.com", "senha123");
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(dto.password())).thenReturn("hashed_senha123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = authService.register(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("joao.qa@test.com");
        assertThat(result.getPassword()).isEqualTo("hashed_senha123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenRegisteringExistingEmail() {
        // Arrange
        RegisterDTO dto = new RegisterDTO("João QA", "joao.qa@test.com", "senha123");
        User existingUser = mock(User.class);
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(existingUser));

        // Act & Assert
        assertThatThrownBy(() -> authService.register(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email já registado no sistema.");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldLoginSuccessfully() {
        // Arrange
        LoginDTO dto = new LoginDTO("joao.qa@test.com", "senha123");
        User user = new User("João QA", "joao.qa@test.com", "hashed_senha123");
        
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(dto.password(), user.getPassword())).thenReturn(true);

        // Act
        User result = authService.login(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("joao.qa@test.com");
    }

    @Test
    void shouldThrowExceptionWhenLoginEmailNotFound() {
        // Arrange
        LoginDTO dto = new LoginDTO("joao.qa@test.com", "senha123");
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Credenciais inválidas.");
    }

    @Test
    void shouldThrowExceptionWhenLoginPasswordIsWrong() {
        // Arrange
        LoginDTO dto = new LoginDTO("joao.qa@test.com", "wrong_password");
        User user = new User("João QA", "joao.qa@test.com", "hashed_senha123");
        
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(dto.password(), user.getPassword())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Credenciais inválidas.");
    }

    @Test
    void shouldUpdatePasswordSuccessfully() {
        // Arrange
        String email = "joao.qa@test.com";
        String oldPassword = "old_password";
        String newPassword = "new_password";
        User user = new User("João QA", email, "hashed_old_password");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(oldPassword, user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("hashed_new_password");

        // Act
        authService.updatePassword(email, oldPassword, newPassword);

        // Assert
        assertThat(user.getPassword()).isEqualTo("hashed_new_password");
        verify(userRepository).save(user);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingPasswordWithWrongOldPassword() {
        // Arrange
        String email = "joao.qa@test.com";
        String oldPassword = "wrong_old_password";
        String newPassword = "new_password";
        User user = new User("João QA", email, "hashed_old_password");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(oldPassword, user.getPassword())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.updatePassword(email, oldPassword, newPassword))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("A password atual está incorreta.");
        
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldUpdateEmailSuccessfully() {
        // Arrange
        String currentEmail = "joao.qa@test.com";
        String newEmail = "joao.novo@test.com";
        User user = new User("João QA", currentEmail, "hashed");

        when(userRepository.findByEmail(newEmail)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(currentEmail)).thenReturn(Optional.of(user));

        // Act
        authService.updateEmail(currentEmail, newEmail);

        // Assert
        assertThat(user.getEmail()).isEqualTo(newEmail);
        verify(userRepository).save(user);
    }
}
