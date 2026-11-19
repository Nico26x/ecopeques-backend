package com.ecopeques.ecopeques_backend.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecopeques.ecopeques_backend.dto.AuthResponse;
import com.ecopeques.ecopeques_backend.dto.LoginRequest;
import com.ecopeques.ecopeques_backend.dto.RegisterRequest;
import com.ecopeques.ecopeques_backend.exception.EmailAlreadyExistsException;
import com.ecopeques.ecopeques_backend.model.Rol;
import com.ecopeques.ecopeques_backend.model.Usuario;
import com.ecopeques.ecopeques_backend.repository.UsuarioRepository;
import com.ecopeques.ecopeques_backend.security.JwtUtils;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(usuarioRepository, passwordEncoder, authenticationManager, jwtUtils);
    }

    @Nested
    @DisplayName("registrar")
    class Registrar {

        @Test
        @DisplayName("registra un DOCENTE normalizando sus datos")
        void registraDocenteNormalizandoDatos() {
            RegisterRequest request = new RegisterRequest(
                    " Ana ",
                    " Perez ",
                    " ANA.PEREZ@EMAIL.COM ",
                    "password123",
                    Rol.DOCENTE
            );

            when(usuarioRepository.existsByEmail("ana.perez@email.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("password-encriptado");
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
                Usuario usuario = invocation.getArgument(0);
                usuario.setId(1L);
                return usuario;
            });
            when(jwtUtils.generateToken(any(Usuario.class))).thenReturn("jwt-token");

            AuthResponse response = usuarioService.registrar(request);

            ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).existsByEmail("ana.perez@email.com");
            verify(passwordEncoder).encode("password123");
            verify(usuarioRepository).save(usuarioCaptor.capture());

            Usuario usuarioGuardado = usuarioCaptor.getValue();
            assertAll(
                    () -> assertEquals("Ana", usuarioGuardado.getNombre()),
                    () -> assertEquals("Perez", usuarioGuardado.getApellido()),
                    () -> assertEquals("ana.perez@email.com", usuarioGuardado.getEmail()),
                    () -> assertEquals("password-encriptado", usuarioGuardado.getPassword()),
                    () -> assertEquals(Rol.DOCENTE, usuarioGuardado.getRol()),
                    () -> assertEquals(1L, response.id()),
                    () -> assertEquals("Ana", response.nombre()),
                    () -> assertEquals("Perez", response.apellido()),
                    () -> assertEquals("ana.perez@email.com", response.email()),
                    () -> assertEquals(Rol.DOCENTE, response.rol()),
                    () -> assertEquals("jwt-token", response.token()),
                    () -> assertFalse(response.token().isBlank())
            );
        }

        @Test
        @DisplayName("rechaza un correo existente después de normalizarlo")
        void rechazaEmailExistenteNormalizado() {
            RegisterRequest request = new RegisterRequest(
                    "Ana", "Perez", " ANA.PEREZ@EMAIL.COM ", "password123", Rol.PADRE
            );
            when(usuarioRepository.existsByEmail("ana.perez@email.com")).thenReturn(true);

            EmailAlreadyExistsException exception = assertThrows(
                    EmailAlreadyExistsException.class,
                    () -> usuarioService.registrar(request)
            );

            assertEquals("El correo ya está registrado", exception.getMessage());
            verify(usuarioRepository).existsByEmail("ana.perez@email.com");
            verify(passwordEncoder, never()).encode(anyString());
            verify(usuarioRepository, never()).save(any(Usuario.class));
            verify(jwtUtils, never()).generateToken(any(Usuario.class));
            verify(authenticationManager, never()).authenticate(any());
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("autentica con el correo normalizado y mapea la respuesta")
        void autenticaConEmailNormalizado() {
            LoginRequest request = new LoginRequest(" ANA.PEREZ@EMAIL.COM ", "password123");
            Usuario usuario = crearUsuario();
            when(usuarioRepository.findByEmail("ana.perez@email.com")).thenReturn(Optional.of(usuario));
            when(jwtUtils.generateToken(usuario)).thenReturn("jwt-token");

            AuthResponse response = usuarioService.login(request);

            ArgumentCaptor<UsernamePasswordAuthenticationToken> authenticationCaptor =
                    ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(authenticationCaptor.capture());
            verify(usuarioRepository).findByEmail("ana.perez@email.com");

            UsernamePasswordAuthenticationToken authentication = authenticationCaptor.getValue();
            assertAll(
                    () -> assertEquals("ana.perez@email.com", authentication.getPrincipal()),
                    () -> assertEquals("password123", authentication.getCredentials()),
                    () -> assertEquals(1L, response.id()),
                    () -> assertEquals("Ana", response.nombre()),
                    () -> assertEquals("Perez", response.apellido()),
                    () -> assertEquals("ana.perez@email.com", response.email()),
                    () -> assertEquals(Rol.PADRE, response.rol()),
                    () -> assertEquals("jwt-token", response.token())
            );
        }

        @Test
        @DisplayName("no consulta ni genera token si las credenciales son inválidas")
        void rechazaCredencialesInvalidas() {
            LoginRequest request = new LoginRequest("ana.perez@email.com", "password123");
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThrows(BadCredentialsException.class, () -> usuarioService.login(request));

            verify(usuarioRepository, never()).findByEmail(anyString());
            verify(jwtUtils, never()).generateToken(any(Usuario.class));
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("lanza error y no genera token si el usuario autenticado no existe")
        void fallaSiUsuarioNoExiste() {
            LoginRequest request = new LoginRequest(" USUARIO@EMAIL.COM ", "password123");
            when(usuarioRepository.findByEmail("usuario@email.com")).thenReturn(Optional.empty());

            UsernameNotFoundException exception = assertThrows(
                    UsernameNotFoundException.class,
                    () -> usuarioService.login(request)
            );

            assertEquals("Usuario no encontrado", exception.getMessage());
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(usuarioRepository).findByEmail("usuario@email.com");
            verify(jwtUtils, never()).generateToken(any(Usuario.class));
            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    private Usuario crearUsuario() {
        return Usuario.builder()
                .id(1L)
                .nombre("Ana")
                .apellido("Perez")
                .email("ana.perez@email.com")
                .password("password-encriptado")
                .rol(Rol.PADRE)
                .build();
    }
}
