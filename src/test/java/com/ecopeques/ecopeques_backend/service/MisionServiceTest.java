package com.ecopeques.ecopeques_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecopeques.ecopeques_backend.dto.MisionResponse;
import com.ecopeques.ecopeques_backend.model.CategoriaMision;
import com.ecopeques.ecopeques_backend.model.Mision;
import com.ecopeques.ecopeques_backend.repository.MisionRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MisionServiceTest {

    @Mock
    private MisionRepository misionRepository;

    private MisionService misionService;

    @BeforeEach
    void setUp() {
        misionService = new MisionService(misionRepository);
    }

    @Test
    void obtenerMisionesActivas_CuandoCategoriaEsNull_DeberiaRetornarTodasLasActivas() {
        Mision agua = crearMision(1L, "Cerrar la llave", CategoriaMision.AGUA, 10);
        Mision energia = crearMision(2L, "Apagar luces", CategoriaMision.ENERGIA, 10);

        when(misionRepository.findByActivaTrueOrderByIdAsc()).thenReturn(List.of(agua, energia));

        List<MisionResponse> response = misionService.obtenerMisionesActivas(null);

        assertEquals(2, response.size());
        assertEquals(1L, response.get(0).id());
        assertEquals("Cerrar la llave", response.get(0).titulo());
        assertEquals(CategoriaMision.AGUA, response.get(0).categoria());
        assertEquals(10, response.get(0).semillasRecompensa());
        assertEquals(2L, response.get(1).id());
        assertEquals(CategoriaMision.ENERGIA, response.get(1).categoria());
        verify(misionRepository).findByActivaTrueOrderByIdAsc();
        verify(misionRepository, never()).findByCategoriaAndActivaTrueOrderByIdAsc(CategoriaMision.AGUA);
    }

    @Test
    void obtenerMisionesActivas_CuandoCategoriaEspecificada_DeberiaFiltrarPorCategoria() {
        Mision reciclaje = crearMision(3L, "Separar residuos", CategoriaMision.RECICLAJE, 20);

        when(misionRepository.findByCategoriaAndActivaTrueOrderByIdAsc(CategoriaMision.RECICLAJE))
                .thenReturn(List.of(reciclaje));

        List<MisionResponse> response = misionService.obtenerMisionesActivas(CategoriaMision.RECICLAJE);

        assertEquals(1, response.size());
        assertEquals(3L, response.get(0).id());
        assertEquals("Separar residuos", response.get(0).titulo());
        assertEquals(CategoriaMision.RECICLAJE, response.get(0).categoria());
        assertEquals(20, response.get(0).semillasRecompensa());
        verify(misionRepository).findByCategoriaAndActivaTrueOrderByIdAsc(CategoriaMision.RECICLAJE);
        verify(misionRepository, never()).findByActivaTrueOrderByIdAsc();
    }

    @Test
    void obtenerMisionesActivas_CuandoNoHayRegistros_DeberiaRetornarListaVacia() {
        when(misionRepository.findByCategoriaAndActivaTrueOrderByIdAsc(CategoriaMision.BIODIVERSIDAD))
                .thenReturn(List.of());

        List<MisionResponse> response = misionService.obtenerMisionesActivas(CategoriaMision.BIODIVERSIDAD);

        assertTrue(response.isEmpty());
        verify(misionRepository).findByCategoriaAndActivaTrueOrderByIdAsc(CategoriaMision.BIODIVERSIDAD);
    }

    private Mision crearMision(Long id, String titulo, CategoriaMision categoria, Integer semillasRecompensa) {
        return Mision.builder()
                .id(id)
                .titulo(titulo)
                .descripcion("Descripción de prueba")
                .categoria(categoria)
                .semillasRecompensa(semillasRecompensa)
                .activa(true)
                .build();
    }
}
