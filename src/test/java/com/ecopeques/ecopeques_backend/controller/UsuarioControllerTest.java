package com.ecopeques.ecopeques_backend.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecopeques.ecopeques_backend.dto.AuthResponse;
import com.ecopeques.ecopeques_backend.dto.LoginRequest;
import com.ecopeques.ecopeques_backend.dto.RegisterRequest;
import com.ecopeques.ecopeques_backend.exception.EmailAlreadyExistsException;
import com.ecopeques.ecopeques_backend.model.Rol;
import com.ecopeques.ecopeques_backend.security.CustomUserDetailsService;
import com.ecopeques.ecopeques_backend.security.JwtUtils;
import com.ecopeques.ecopeques_backend.service.UsuarioService;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;

@WebMvcTest(UsuarioController.class)
@ImportAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
@Import(ControllerSecurityTestConfig.class)
@DisplayName("UsuarioController")
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Nested
    @DisplayName("registro")
    class Registro {

        @Test
        @DisplayName("crea el usuario y devuelve 201")
        void registrarValido() throws Exception {
            AuthResponse response = new AuthResponse(
                    1L, "Ana", "Perez", "ana@email.com", Rol.PADRE, "jwt-token"
            );
            when(usuarioService.registrar(any(RegisterRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/register").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registroValido()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nombre").value("Ana"))
                    .andExpect(jsonPath("$.apellido").value("Perez"))
                    .andExpect(jsonPath("$.email").value("ana@email.com"))
                    .andExpect(jsonPath("$.rol").value("PADRE"))
                    .andExpect(jsonPath("$.token").value("jwt-token"));
        }

        @Test
        @DisplayName("devuelve 409 cuando el correo ya existe")
        void correoDuplicado() throws Exception {
            when(usuarioService.registrar(any(RegisterRequest.class)))
                    .thenThrow(new EmailAlreadyExistsException("El correo ya está registrado"));

            mockMvc.perform(post("/api/auth/register").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registroValido()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("El correo ya está registrado"));
        }

        @ParameterizedTest(name = "rechaza {0}")
        @MethodSource("casosInvalidos")
        @DisplayName("aplica Bean Validation y no invoca el servicio")
        void registroInvalido(String caso, String json, String campo, String mensaje) throws Exception {
            mockMvc.perform(post("/api/auth/register").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> assertFieldError(result.getResolvedException(), campo, mensaje));

            verifyNoInteractions(usuarioService);
        }

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    Arguments.of("nombre vacío", registro("", "Perez", "ana@email.com", "password", "PADRE"),
                            "nombre", "El nombre es obligatorio"),
                    Arguments.of("nombre mayor a 100", registro("A".repeat(101), "Perez", "ana@email.com",
                            "password", "PADRE"), "nombre", "El nombre no puede superar los 100 caracteres"),
                    Arguments.of("apellido vacío", registro("Ana", " ", "ana@email.com", "password", "PADRE"),
                            "apellido", "El apellido es obligatorio"),
                    Arguments.of("correo inválido", registro("Ana", "Perez", "correo-invalido", "password",
                            "PADRE"), "email", "El correo debe tener un formato válido"),
                    Arguments.of("contraseña de 7 caracteres", registro("Ana", "Perez", "ana@email.com",
                            "1234567", "PADRE"), "password", "La contraseña debe tener entre 8 y 100 caracteres"),
                    Arguments.of("contraseña de 101 caracteres", registro("Ana", "Perez", "ana@email.com",
                            "A".repeat(101), "PADRE"), "password",
                            "La contraseña debe tener entre 8 y 100 caracteres"),
                    Arguments.of("rol nulo", registro("Ana", "Perez", "ana@email.com", "password", null),
                            "rol", "El rol es obligatorio")
            );
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("devuelve el usuario y token con 200")
        void loginValido() throws Exception {
            AuthResponse response = new AuthResponse(
                    1L, "Ana", "Perez", "ana@email.com", Rol.PADRE, "jwt-token"
            );
            when(usuarioService.login(any(LoginRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/login").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginValidoJson()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value("ana@email.com"))
                    .andExpect(jsonPath("$.rol").value("PADRE"))
                    .andExpect(jsonPath("$.token").value("jwt-token"));
        }

        @Test
        @DisplayName("normaliza credenciales incorrectas a 401")
        void credencialesInvalidas() throws Exception {
            when(usuarioService.login(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/api/auth/login").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginValidoJson()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
        }

        @ParameterizedTest(name = "rechaza {0}")
        @MethodSource("casosInvalidos")
        @DisplayName("aplica Bean Validation y no invoca el servicio")
        void loginInvalido(String caso, String json, String campo, String mensaje) throws Exception {
            mockMvc.perform(post("/api/auth/login").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> assertFieldError(result.getResolvedException(), campo, mensaje));

            verifyNoInteractions(usuarioService);
        }

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    Arguments.of("correo vacío", login(" ", "password"), "email", "El correo es obligatorio"),
                    Arguments.of("correo inválido", login("correo-invalido", "password"), "email",
                            "El correo debe tener un formato válido"),
                    Arguments.of("contraseña vacía", login("ana@email.com", ""), "password",
                            "La contraseña es obligatoria"),
                    Arguments.of("contraseña de 7 caracteres", login("ana@email.com", "1234567"), "password",
                            "La contraseña debe tener entre 8 y 100 caracteres")
            );
        }
    }

    private static String registroValido() {
        return registro("Ana", "Perez", "ana@email.com", "password", "PADRE");
    }

    private static String registro(String nombre, String apellido, String email, String password, String rol) {
        String rolJson = rol == null ? "null" : "\"" + rol + "\"";
        return """
                {"nombre":"%s","apellido":"%s","email":"%s","password":"%s","rol":%s}
                """.formatted(nombre, apellido, email, password, rolJson);
    }

    private static String loginValidoJson() {
        return login("ana@email.com", "password");
    }

    private static String login(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    private static void assertFieldError(Exception exception, String field, String message) {
        MethodArgumentNotValidException validationException = (MethodArgumentNotValidException) exception;
        assertTrue(validationException.getBindingResult().getFieldErrors(field).stream()
                .anyMatch(error -> message.equals(error.getDefaultMessage())));
    }
}
