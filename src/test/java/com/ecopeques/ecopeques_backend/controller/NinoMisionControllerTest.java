package com.ecopeques.ecopeques_backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecopeques.ecopeques_backend.dto.AsignarMisionRequest;
import com.ecopeques.ecopeques_backend.dto.ConfirmarMisionRequest;
import com.ecopeques.ecopeques_backend.dto.MisionAcreditadaResponse;
import com.ecopeques.ecopeques_backend.dto.MisionAsignadaResponse;
import com.ecopeques.ecopeques_backend.exception.EvidenciaInvalidaException;
import com.ecopeques.ecopeques_backend.exception.LimiteMisionesAlcanzadoException;
import com.ecopeques.ecopeques_backend.exception.MisionNoAcreditableException;
import com.ecopeques.ecopeques_backend.exception.MisionNoConfirmableException;
import com.ecopeques.ecopeques_backend.exception.MisionNotFoundException;
import com.ecopeques.ecopeques_backend.exception.MisionYaAsignadaException;
import com.ecopeques.ecopeques_backend.exception.NinoMisionNotFoundException;
import com.ecopeques.ecopeques_backend.exception.NinoNotFoundException;
import com.ecopeques.ecopeques_backend.model.CategoriaMision;
import com.ecopeques.ecopeques_backend.model.EstadoMisionAsignada;
import com.ecopeques.ecopeques_backend.security.CustomUserDetailsService;
import com.ecopeques.ecopeques_backend.security.JwtUtils;
import com.ecopeques.ecopeques_backend.service.NinoMisionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

@WebMvcTest(NinoMisionController.class)
@ImportAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
@Import(ControllerSecurityTestConfig.class)
@DisplayName("NinoMisionController")
class NinoMisionControllerTest {

    private static final String TUTOR = "tutor@email.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NinoMisionService ninoMisionService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Nested
    @DisplayName("HU-05 - asignar misión")
    class AsignarMision {

        @Test
        @WithMockUser(username = TUTOR)
        @DisplayName("asigna con fecha límite pasada porque el DTO no la restringe")
        void asignarValida() throws Exception {
            LocalDate fechaLimite = LocalDate.of(2020, 1, 1);
            MisionAsignadaResponse response = asignada(EstadoMisionAsignada.EN_PROGRESO, fechaLimite, null, false);
            when(ninoMisionService.asignarMision(any(AsignarMisionRequest.class), any(String.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/misiones/asignar").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"ninoId":10,"misionId":20,"fechaLimite":"2020-01-01"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(100))
                    .andExpect(jsonPath("$.ninoId").value(10))
                    .andExpect(jsonPath("$.misionId").value(20))
                    .andExpect(jsonPath("$.tituloMision").value("Cerrar la llave"))
                    .andExpect(jsonPath("$.categoria").value("AGUA"))
                    .andExpect(jsonPath("$.semillasRecompensa").value(10))
                    .andExpect(jsonPath("$.estado").value("EN_PROGRESO"))
                    .andExpect(jsonPath("$.fechaLimite").value("2020-01-01"))
                    .andExpect(jsonPath("$.acreditada").value(false));

            verify(ninoMisionService).asignarMision(
                    new AsignarMisionRequest(10L, 20L, fechaLimite), TUTOR
            );
        }

        @ParameterizedTest(name = "rechaza {0}")
        @MethodSource("idsObligatorios")
        @WithMockUser(username = TUTOR)
        @DisplayName("valida únicamente IDs obligatorios y no invoca el servicio")
        void idsInvalidos(String caso, String json, String campo, String mensaje) throws Exception {
            mockMvc.perform(post("/api/misiones/asignar").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> assertFieldError(result.getResolvedException(), campo, mensaje));

            verifyNoInteractions(ninoMisionService);
        }

        static Stream<Arguments> idsObligatorios() {
            return Stream.of(
                    Arguments.of("ninoId nulo", "{\"ninoId\":null,\"misionId\":20}", "ninoId",
                            "El id del niño es obligatorio"),
                    Arguments.of("misionId nulo", "{\"ninoId\":10,\"misionId\":null}", "misionId",
                            "El id de la misión es obligatorio")
            );
        }

        @Test
        @DisplayName("devuelve 401 sin autenticación y no invoca el servicio")
        void sinAutenticacion() throws Exception {
            mockMvc.perform(post("/api/misiones/asignar").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ninoId\":10,\"misionId\":20}"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(ninoMisionService);
        }

        @ParameterizedTest(name = "devuelve 404: {0}")
        @MethodSource("noEncontrados")
        @WithMockUser(username = TUTOR)
        @DisplayName("conserva mensajes de recursos no encontrados")
        void recursoNoEncontrado(String mensaje, RuntimeException exception) throws Exception {
            when(ninoMisionService.asignarMision(any(AsignarMisionRequest.class), any(String.class)))
                    .thenThrow(exception);

            mockMvc.perform(post("/api/misiones/asignar").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ninoId\":10,\"misionId\":20}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(mensaje));
        }

        static Stream<Arguments> noEncontrados() {
            return Stream.of(
                    Arguments.of("Niño no encontrado", new NinoNotFoundException("Niño no encontrado")),
                    Arguments.of("Misión no encontrada", new MisionNotFoundException("Misión no encontrada")),
                    Arguments.of("Misión no encontrada o inactiva",
                            new MisionNotFoundException("Misión no encontrada o inactiva"))
            );
        }

        @Test
        @WithMockUser(username = TUTOR)
        @DisplayName("devuelve 403 uniforme para un niño ajeno")
        void ninoAjeno() throws Exception {
            when(ninoMisionService.asignarMision(any(AsignarMisionRequest.class), any(String.class)))
                    .thenThrow(new AccessDeniedException("No tienes permisos para asignar misiones a este niño"));

            mockMvc.perform(post("/api/misiones/asignar").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ninoId\":10,\"misionId\":20}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message")
                            .value("No tienes permisos para acceder a este recurso"));
        }

        @ParameterizedTest(name = "devuelve 409: {0}")
        @MethodSource("conflictos")
        @WithMockUser(username = TUTOR)
        @DisplayName("conserva mensajes de conflictos de asignación")
        void conflicto(String mensaje, RuntimeException exception) throws Exception {
            when(ninoMisionService.asignarMision(any(AsignarMisionRequest.class), any(String.class)))
                    .thenThrow(exception);

            mockMvc.perform(post("/api/misiones/asignar").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ninoId\":10,\"misionId\":20}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(mensaje));
        }

        static Stream<Arguments> conflictos() {
            return Stream.of(
                    Arguments.of("La misión ya está asignada al niño en estado EN_PROGRESO",
                            new MisionYaAsignadaException(
                                    "La misión ya está asignada al niño en estado EN_PROGRESO")),
                    Arguments.of("El niño ya tiene 3 misiones activas en progreso",
                            new LimiteMisionesAlcanzadoException(
                                    "El niño ya tiene 3 misiones activas en progreso"))
            );
        }
    }

    @Nested
    @DisplayName("HU-06 - confirmar y listar misiones")
    class ConfirmarYListar {

        @Test
        @WithMockUser(username = TUTOR)
        @DisplayName("confirma sin body, permitido por el contrato actual")
        void confirmarSinBody() throws Exception {
            MisionAsignadaResponse response = asignada(
                    EstadoMisionAsignada.COMPLETADA, null, null, false
            );
            when(ninoMisionService.confirmarMision(100L, null, TUTOR)).thenReturn(response);

            mockMvc.perform(put("/api/misiones-ninos/100/confirmar").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100))
                    .andExpect(jsonPath("$.estado").value("COMPLETADA"))
                    .andExpect(jsonPath("$.evidencia").doesNotExist())
                    .andExpect(jsonPath("$.acreditada").value(false));

            verify(ninoMisionService).confirmarMision(100L, null, TUTOR);
        }

        @Test
        @WithMockUser(username = TUTOR)
        @DisplayName("pasa la evidencia al servicio")
        void confirmarConEvidencia() throws Exception {
            String evidencia = "https://example.com/evidencia.jpg";
            when(ninoMisionService.confirmarMision(100L, new ConfirmarMisionRequest(evidencia), TUTOR))
                    .thenReturn(asignada(EstadoMisionAsignada.COMPLETADA, null, evidencia, false));

            mockMvc.perform(put("/api/misiones-ninos/100/confirmar").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"evidencia\":\"https://example.com/evidencia.jpg\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.evidencia").value(evidencia));
        }

        @Test
        @WithMockUser(username = TUTOR)
        @DisplayName("devuelve 400 con el mensaje de evidencia inválida")
        void evidenciaInvalida() throws Exception {
            when(ninoMisionService.confirmarMision(100L, new ConfirmarMisionRequest("archivo.jpg"), TUTOR))
                    .thenThrow(new EvidenciaInvalidaException(
                            "Formato de evidencia inválido. Debe ser una URL válida o una imagen en Base64"
                    ));

            mockMvc.perform(put("/api/misiones-ninos/100/confirmar").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"evidencia\":\"archivo.jpg\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            "Formato de evidencia inválido. Debe ser una URL válida o una imagen en Base64"
                    ));
        }

        @Test
        @WithMockUser(username = TUTOR)
        @DisplayName("devuelve 404 cuando la asignación no existe")
        void asignacionNoExiste() throws Exception {
            when(ninoMisionService.confirmarMision(100L, null, TUTOR))
                    .thenThrow(new NinoMisionNotFoundException("La asignación de misión no existe"));

            mockMvc.perform(put("/api/misiones-ninos/100/confirmar").with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("La asignación de misión no existe"));
        }

        @Test
        @WithMockUser(username = TUTOR)
        @DisplayName("devuelve 403 uniforme para una asignación ajena")
        void asignacionAjena() throws Exception {
            when(ninoMisionService.confirmarMision(100L, null, TUTOR))
                    .thenThrow(new AccessDeniedException("No tienes permiso para confirmar esta misión"));

            mockMvc.perform(put("/api/misiones-ninos/100/confirmar").with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message")
                            .value("No tienes permisos para acceder a este recurso"));
        }

        @Test
        @WithMockUser(username = TUTOR)
        @DisplayName("devuelve 409 cuando la misión no es confirmable")
        void noConfirmable() throws Exception {
            String mensaje = "La misión asignada ya fue completada o no está en progreso";
            when(ninoMisionService.confirmarMision(100L, null, TUTOR))
                    .thenThrow(new MisionNoConfirmableException(mensaje));

            mockMvc.perform(put("/api/misiones-ninos/100/confirmar").with(csrf()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(mensaje));
        }

        @Test
        @WithMockUser(username = TUTOR)
        @DisplayName("rechaza id no numérico sin invocar el servicio")
        void idNoNumerico() throws Exception {
            mockMvc.perform(put("/api/misiones-ninos/no-numerico/confirmar").with(csrf()))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(ninoMisionService);
        }

        @Test
        @DisplayName("rechaza confirmación sin autenticación sin invocar el servicio")
        void confirmarSinAutenticacion() throws Exception {
            mockMvc.perform(put("/api/misiones-ninos/100/confirmar").with(csrf()))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(ninoMisionService);
        }

        @Test
        @WithMockUser(username = TUTOR)
        @DisplayName("lista pendientes con 200")
        void listarPendientes() throws Exception {
            when(ninoMisionService.listarPendientesPorTutor(TUTOR)).thenReturn(List.of(
                    asignada(EstadoMisionAsignada.EN_PROGRESO, LocalDate.of(2026, 12, 1), null, false)
            ));

            mockMvc.perform(get("/api/misiones-ninos/tutor/pendientes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(100))
                    .andExpect(jsonPath("$[0].estado").value("EN_PROGRESO"));

            verify(ninoMisionService).listarPendientesPorTutor(TUTOR);
        }

        @Test
        @DisplayName("rechaza listado sin autenticación sin invocar el servicio")
        void listarSinAutenticacion() throws Exception {
            mockMvc.perform(get("/api/misiones-ninos/tutor/pendientes"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(ninoMisionService);
        }
    }

    @Nested
    @DisplayName("HU-07 - acreditar misión")
    class AcreditarMision {

        @Test
        @WithMockUser(username = TUTOR)
        @DisplayName("acredita y devuelve las semillas con 200")
        void acreditarValida() throws Exception {
            MisionAcreditadaResponse response = new MisionAcreditadaResponse(100L, 10L, 20L, 15, 40, true);
            when(ninoMisionService.acreditarMision(100L, TUTOR)).thenReturn(response);

            mockMvc.perform(put("/api/misiones-ninos/100/acreditar").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100))
                    .andExpect(jsonPath("$.ninoId").value(10))
                    .andExpect(jsonPath("$.misionId").value(20))
                    .andExpect(jsonPath("$.semillasAcreditadas").value(15))
                    .andExpect(jsonPath("$.totalSemillas").value(40))
                    .andExpect(jsonPath("$.acreditada").value(true));

            verify(ninoMisionService).acreditarMision(100L, TUTOR);
        }

        @Test
        @DisplayName("devuelve 401 sin autenticación y no invoca el servicio")
        void sinAutenticacion() throws Exception {
            mockMvc.perform(put("/api/misiones-ninos/100/acreditar").with(csrf()))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(ninoMisionService);
        }

        @Test
        @WithMockUser(username = TUTOR)
        @DisplayName("devuelve 404 cuando la asignación no existe")
        void asignacionNoExiste() throws Exception {
            when(ninoMisionService.acreditarMision(100L, TUTOR))
                    .thenThrow(new NinoMisionNotFoundException("La asignación de misión no existe"));

            mockMvc.perform(put("/api/misiones-ninos/100/acreditar").with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("La asignación de misión no existe"));
        }

        @Test
        @WithMockUser(username = TUTOR)
        @DisplayName("devuelve 403 uniforme para una asignación ajena")
        void asignacionAjena() throws Exception {
            when(ninoMisionService.acreditarMision(100L, TUTOR))
                    .thenThrow(new AccessDeniedException("No tienes permiso para acreditar esta misión"));

            mockMvc.perform(put("/api/misiones-ninos/100/acreditar").with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message")
                            .value("No tienes permisos para acceder a este recurso"));
        }

        @ParameterizedTest(name = "devuelve 409: {0}")
        @MethodSource("noAcreditables")
        @WithMockUser(username = TUTOR)
        @DisplayName("conserva mensajes cuando no se puede acreditar")
        void noAcreditable(String mensaje) throws Exception {
            when(ninoMisionService.acreditarMision(100L, TUTOR))
                    .thenThrow(new MisionNoAcreditableException(mensaje));

            mockMvc.perform(put("/api/misiones-ninos/100/acreditar").with(csrf()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(mensaje));
        }

        static Stream<String> noAcreditables() {
            return Stream.of(
                    "La misión debe estar completada antes de acreditarla",
                    "La misión ya fue acreditada"
            );
        }

        @Test
        @WithMockUser(username = TUTOR)
        @DisplayName("rechaza id no numérico sin invocar el servicio")
        void idNoNumerico() throws Exception {
            mockMvc.perform(put("/api/misiones-ninos/no-numerico/acreditar").with(csrf()))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(ninoMisionService);
        }
    }

    private static MisionAsignadaResponse asignada(
            EstadoMisionAsignada estado,
            LocalDate fechaLimite,
            String evidencia,
            boolean acreditada
    ) {
        return new MisionAsignadaResponse(
                100L,
                10L,
                20L,
                "Cerrar la llave",
                CategoriaMision.AGUA,
                10,
                estado,
                LocalDateTime.of(2026, 11, 14, 10, 0),
                estado == EstadoMisionAsignada.COMPLETADA
                        ? LocalDateTime.of(2026, 11, 14, 11, 0)
                        : null,
                fechaLimite,
                evidencia,
                acreditada
        );
    }

    private static void assertFieldError(Exception exception, String field, String message) {
        MethodArgumentNotValidException validationException = (MethodArgumentNotValidException) exception;
        assertEquals(message, validationException.getBindingResult().getFieldError(field).getDefaultMessage());
    }
}
