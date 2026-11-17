package com.ecopeques.ecopeques_backend.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecopeques.ecopeques_backend.dto.MisionResponse;
import com.ecopeques.ecopeques_backend.model.CategoriaMision;
import com.ecopeques.ecopeques_backend.security.CustomUserDetailsService;
import com.ecopeques.ecopeques_backend.security.JwtUtils;
import com.ecopeques.ecopeques_backend.service.MisionService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MisionController.class)
@ImportAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
@Import(ControllerSecurityTestConfig.class)
@DisplayName("MisionController")
class MisionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MisionService misionService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "tutor@email.com")
    @DisplayName("lista todas las misiones activas con 200")
    void listarSinCategoria() throws Exception {
        when(misionService.obtenerMisionesActivas(null)).thenReturn(List.of(
                new MisionResponse(20L, "Cerrar la llave", "Ahorra agua", CategoriaMision.AGUA, 10)
        ));

        mockMvc.perform(get("/api/misiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(20))
                .andExpect(jsonPath("$[0].titulo").value("Cerrar la llave"))
                .andExpect(jsonPath("$[0].descripcion").value("Ahorra agua"))
                .andExpect(jsonPath("$[0].categoria").value("AGUA"))
                .andExpect(jsonPath("$[0].semillasRecompensa").value(10));

        verify(misionService).obtenerMisionesActivas(null);
    }

    @Test
    @WithMockUser(username = "tutor@email.com")
    @DisplayName("pasa la categoría al servicio")
    void listarPorCategoria() throws Exception {
        when(misionService.obtenerMisionesActivas(CategoriaMision.RECICLAJE)).thenReturn(List.of());

        mockMvc.perform(get("/api/misiones").param("categoria", "RECICLAJE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(misionService).obtenerMisionesActivas(CategoriaMision.RECICLAJE);
    }

    @Test
    @WithMockUser(username = "tutor@email.com")
    @DisplayName("devuelve 400 para una categoría inválida sin invocar el servicio")
    void categoriaInvalida() throws Exception {
        mockMvc.perform(get("/api/misiones").param("categoria", "OTRA"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(misionService);
    }

    @Test
    @DisplayName("devuelve 401 sin autenticación y no invoca el servicio")
    void sinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/misiones"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(misionService);
    }
}
