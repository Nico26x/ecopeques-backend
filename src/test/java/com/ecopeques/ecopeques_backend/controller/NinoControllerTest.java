package com.ecopeques.ecopeques_backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecopeques.ecopeques_backend.dto.NinoRequest;
import com.ecopeques.ecopeques_backend.dto.NinoResponse;
import com.ecopeques.ecopeques_backend.security.CustomUserDetailsService;
import com.ecopeques.ecopeques_backend.security.JwtUtils;
import com.ecopeques.ecopeques_backend.service.NinoService;
import java.util.List;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;

@WebMvcTest(NinoController.class)
@ImportAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
@Import(ControllerSecurityTestConfig.class)
@DisplayName("NinoController")
class NinoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NinoService ninoService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Nested
    @DisplayName("crear niño")
    class CrearNino {

        @Test
        @WithMockUser(username = "tutor@email.com")
        @DisplayName("crea y serializa el niño con 201")
        void crearValido() throws Exception {
            NinoResponse response = new NinoResponse(10L, "Lucas", 0, "avatar.png", "Grupo A", 1L);
            when(ninoService.crearNino(any(NinoRequest.class), any(String.class))).thenReturn(response);

            mockMvc.perform(post("/api/ninos").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(ninoJson("Lucas", "avatar.png", "Grupo A")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.nombre").value("Lucas"))
                    .andExpect(jsonPath("$.totalSemillas").value(0))
                    .andExpect(jsonPath("$.avatar").value("avatar.png"))
                    .andExpect(jsonPath("$.grupo").value("Grupo A"))
                    .andExpect(jsonPath("$.tutorId").value(1));

            verify(ninoService).crearNino(new NinoRequest("Lucas", "avatar.png", "Grupo A"), "tutor@email.com");
        }

        @Test
        @DisplayName("devuelve 401 sin autenticación y no invoca el servicio")
        void crearSinAutenticacion() throws Exception {
            mockMvc.perform(post("/api/ninos").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(ninoJson("Lucas", null, null)))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(ninoService);
        }

        @ParameterizedTest(name = "rechaza {0}")
        @MethodSource("casosInvalidos")
        @WithMockUser(username = "tutor@email.com")
        @DisplayName("aplica límites DTO y no invoca el servicio")
        void crearInvalido(String caso, String json, String campo, String mensaje) throws Exception {
            mockMvc.perform(post("/api/ninos").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> assertFieldError(result.getResolvedException(), campo, mensaje));

            verifyNoInteractions(ninoService);
        }

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    Arguments.of("nombre vacío", ninoJson(" ", null, null), "nombre",
                            "El nombre del niño es obligatorio"),
                    Arguments.of("nombre mayor a 100", ninoJson("A".repeat(101), null, null), "nombre",
                            "El nombre no puede superar los 100 caracteres"),
                    Arguments.of("avatar mayor a 255", ninoJson("Lucas", "A".repeat(256), null), "avatar",
                            "El avatar no puede superar los 255 caracteres"),
                    Arguments.of("grupo mayor a 100", ninoJson("Lucas", null, "A".repeat(101)), "grupo",
                            "El grupo no puede superar los 100 caracteres")
            );
        }
    }

    @Nested
    @DisplayName("listar por tutor")
    class ListarPorTutor {

        @Test
        @WithMockUser(username = "tutor@email.com")
        @DisplayName("devuelve la lista con 200")
        void listar() throws Exception {
            when(ninoService.obtenerNinosPorTutor(1L, "tutor@email.com")).thenReturn(List.of(
                    new NinoResponse(10L, "Lucas", 5, null, null, 1L),
                    new NinoResponse(11L, "Sofia", 8, "sofia.png", "Grupo B", 1L)
            ));

            mockMvc.perform(get("/api/ninos/tutor/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(10))
                    .andExpect(jsonPath("$[0].totalSemillas").value(5))
                    .andExpect(jsonPath("$[1].nombre").value("Sofia"));
        }

        @Test
        @WithMockUser(username = "tutor@email.com")
        @DisplayName("devuelve el mensaje uniforme con 403")
        void listarTutorAjeno() throws Exception {
            when(ninoService.obtenerNinosPorTutor(2L, "tutor@email.com"))
                    .thenThrow(new AccessDeniedException("detalle interno"));

            mockMvc.perform(get("/api/ninos/tutor/2"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message")
                            .value("No tienes permisos para acceder a este recurso"));
        }

        @Test
        @WithMockUser(username = "tutor@email.com")
        @DisplayName("devuelve 400 para un id no numérico sin invocar el servicio")
        void listarIdNoNumerico() throws Exception {
            mockMvc.perform(get("/api/ninos/tutor/no-numerico"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(ninoService);
        }

        @Test
        @DisplayName("devuelve 401 sin autenticación y no invoca el servicio")
        void listarSinAutenticacion() throws Exception {
            mockMvc.perform(get("/api/ninos/tutor/1"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(ninoService);
        }
    }

    private static String ninoJson(String nombre, String avatar, String grupo) {
        return """
                {"nombre":%s,"avatar":%s,"grupo":%s}
                """.formatted(jsonString(nombre), jsonString(avatar), jsonString(grupo));
    }

    private static String jsonString(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static void assertFieldError(Exception exception, String field, String message) {
        MethodArgumentNotValidException validationException = (MethodArgumentNotValidException) exception;
        assertEquals(message, validationException.getBindingResult().getFieldError(field).getDefaultMessage());
    }
}
