package com.ecopeques.ecopeques_backend.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecopeques.ecopeques_backend.dto.MisionAcreditadaResponse;
import com.ecopeques.ecopeques_backend.exception.MisionNoAcreditableException;
import com.ecopeques.ecopeques_backend.exception.NinoMisionNotFoundException;
import com.ecopeques.ecopeques_backend.security.CustomUserDetailsService;
import com.ecopeques.ecopeques_backend.security.JwtUtils;
import com.ecopeques.ecopeques_backend.service.NinoMisionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NinoMisionController.class)
class NinoMisionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NinoMisionService ninoMisionService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "tutor@email.com")
    void acreditarMision_CuandoEsValida_DeberiaRetornarRespuestaCompleta() throws Exception {
        MisionAcreditadaResponse response = new MisionAcreditadaResponse(
                100L,
                10L,
                20L,
                15,
                40,
                true
        );
        when(ninoMisionService.acreditarMision(100L, "tutor@email.com")).thenReturn(response);

        mockMvc.perform(put("/api/misiones-ninos/100/acreditar").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.ninoId").value(10))
                .andExpect(jsonPath("$.misionId").value(20))
                .andExpect(jsonPath("$.semillasAcreditadas").value(15))
                .andExpect(jsonPath("$.totalSemillas").value(40))
                .andExpect(jsonPath("$.acreditada").value(true));

        verify(ninoMisionService).acreditarMision(100L, "tutor@email.com");
    }

    @Test
    void acreditarMision_SinAutenticacion_DeberiaRetornarUnauthorized() throws Exception {
        mockMvc.perform(put("/api/misiones-ninos/100/acreditar").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "tutor@email.com")
    void acreditarMision_CuandoAsignacionNoExiste_DeberiaRetornarNotFound() throws Exception {
        when(ninoMisionService.acreditarMision(100L, "tutor@email.com"))
                .thenThrow(new NinoMisionNotFoundException("La asignación de misión no existe"));

        mockMvc.perform(put("/api/misiones-ninos/100/acreditar").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La asignación de misión no existe"));
    }

    @Test
    @WithMockUser(username = "tutor@email.com")
    void acreditarMision_CuandoAsignacionEsAjena_DeberiaRetornarForbidden() throws Exception {
        when(ninoMisionService.acreditarMision(100L, "tutor@email.com"))
                .thenThrow(new AccessDeniedException("No tienes permiso para acreditar esta misión"));

        mockMvc.perform(put("/api/misiones-ninos/100/acreditar").with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tienes permisos para acceder a este recurso"));
    }

    @Test
    @WithMockUser(username = "tutor@email.com")
    void acreditarMision_CuandoEstaEnProgreso_DeberiaRetornarConflict() throws Exception {
        when(ninoMisionService.acreditarMision(100L, "tutor@email.com"))
                .thenThrow(new MisionNoAcreditableException(
                        "La misión debe estar completada antes de acreditarla"
                ));

        mockMvc.perform(put("/api/misiones-ninos/100/acreditar").with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "La misión debe estar completada antes de acreditarla"
                ));
    }

    @Test
    @WithMockUser(username = "tutor@email.com")
    void acreditarMision_CuandoYaFueAcreditada_DeberiaRetornarConflict() throws Exception {
        when(ninoMisionService.acreditarMision(100L, "tutor@email.com"))
                .thenThrow(new MisionNoAcreditableException("La misión ya fue acreditada"));

        mockMvc.perform(put("/api/misiones-ninos/100/acreditar").with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La misión ya fue acreditada"));
    }

    @Test
    @WithMockUser(username = "tutor@email.com")
    void acreditarMision_CuandoIdNoEsNumerico_DeberiaRetornarBadRequest() throws Exception {
        mockMvc.perform(put("/api/misiones-ninos/no-numerico/acreditar").with(csrf()))
                .andExpect(status().isBadRequest());
    }
}
