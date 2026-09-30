package com.musa.users.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import com.musa.users.dto.request.RegisterRequestDto;
import com.musa.users.exception.ResourceNotFoundException;
import com.musa.users.exception.UserAlreadyExistsException;
import com.musa.users.repository.RoleRepository;
import com.musa.users.repository.UserRepository;
import com.musa.users.service.EmailService;
import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.entity.Role;
import com.musa.users.entity.User;

@ExtendWith(MockitoExtension.class)
class RegisterServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private RegisterServiceImpl registerService;

    private RegisterRequestDto request;

    @BeforeEach
    void setUp() {
        request = new RegisterRequestDto("Ana López", "ana@usi.com","Password123!","USER");
    }

    @Test
    void register_debeLanzarExcepcion_siCorreoYaExiste() {
        // Arrange
        RegisterRequestDto request = new RegisterRequestDto(
                "Ana López",
                "ana@usi.com",
                "Password123!",
                "USER");

        // Act
        when(userRepository.existsByEmail("ana@usi.com")).thenReturn(true);

        // Assert
        assertThrows(UserAlreadyExistsException.class, () -> registerService.register(request));

        verify(userRepository).existsByEmail("ana@usi.com");
        verifyNoInteractions(roleRepository, passwordEncoder, emailService);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_debeLanzarExcepcion_siRolNoExiste() {

        when(userRepository.existsByEmail(request.email())).thenReturn(false);

        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,() -> registerService.register(request));

        verify(userRepository).existsByEmail(request.email());
        verify(roleRepository).findByName("USER");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, emailService);
    }

    @Test
    void register_debeRegistrarUsuario_siDatosSonValidos() {

        Role role = new Role();
        role.setId(1L);
        role.setName("USER");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);

        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));

        when(passwordEncoder.encode("Password123!")).thenReturn("HASH_SIMULADO");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponseDto response = registerService.register(request);

        assertEquals("Registro exitoso.", response.message());

        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(any(User.class));
        verify(emailService).sendWelcomeEmail("ana@usi.com", "Ana López");
    }

    @Test
    void register_debeGuardarUsuarioConAtributosCorrectos() {

        Role role = new Role();
        role.setId(1L);
        role.setName("USER");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);

        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));

        when(passwordEncoder.encode(request.password())).thenReturn("HASH_SIMULADO");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        registerService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();

        assertAll(() -> assertEquals("Ana López", savedUser.getFullName()),
                () -> assertEquals("ana@usi.com", savedUser.getEmail()),
                () -> assertEquals("HASH_SIMULADO", savedUser.getPasswordHash()),
                () -> assertSame(role, savedUser.getRole()),
                () -> assertTrue(savedUser.getIsActive()));
    }
}
