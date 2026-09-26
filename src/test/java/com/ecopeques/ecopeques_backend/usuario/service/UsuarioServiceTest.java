package com.ecopeques.ecopeques_backend.usuario.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecopeques.ecopeques_backend.shared.exception.EmailAlreadyExistsException;
import com.ecopeques.ecopeques_backend.usuario.domain.Rol;
import com.ecopeques.ecopeques_backend.usuario.domain.Usuario;
import com.ecopeques.ecopeques_backend.usuario.dto.AuthResponse;
import com.ecopeques.ecopeques_backend.usuario.dto.RegisterRequest;
import com.ecopeques.ecopeques_backend.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(usuarioRepository, passwordEncoder);
    }

    @Test
    void registrar_CuandoEmailNoExiste_DeberiaGuardarUsuarioYRetornarResponse() {
        RegisterRequest request = new RegisterRequest(
                " Ana ",
                " Perez ",
                " ANA.PEREZ@EMAIL.COM ",
                "password123",
                Rol.PADRE
        );

        when(usuarioRepository.existsByEmail("ana.perez@email.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("password-encriptado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(1L);
            return usuario;
        });

        AuthResponse response = usuarioService.registrar(request);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());

        Usuario usuarioGuardado = usuarioCaptor.getValue();
        assertEquals("Ana", usuarioGuardado.getNombre());
        assertEquals("Perez", usuarioGuardado.getApellido());
        assertEquals("ana.perez@email.com", usuarioGuardado.getEmail());
        assertEquals("password-encriptado", usuarioGuardado.getPassword());
        assertEquals(Rol.PADRE, usuarioGuardado.getRol());

        assertEquals(1L, response.id());
        assertEquals("Ana", response.nombre());
        assertEquals("Perez", response.apellido());
        assertEquals("ana.perez@email.com", response.email());
        assertEquals(Rol.PADRE, response.rol());
    }

    @Test
    void registrar_CuandoEmailYaExiste_DeberiaLanzarEmailAlreadyExistsException() {
        RegisterRequest request = new RegisterRequest(
                "Ana",
                "Perez",
                "ana.perez@email.com",
                "password123",
                Rol.PADRE
        );

        when(usuarioRepository.existsByEmail("ana.perez@email.com")).thenReturn(true);

        EmailAlreadyExistsException exception = assertThrows(
                EmailAlreadyExistsException.class,
                () -> usuarioService.registrar(request)
        );

        assertEquals("El correo ya está registrado", exception.getMessage());
        verify(passwordEncoder, never()).encode(anyString());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
}
