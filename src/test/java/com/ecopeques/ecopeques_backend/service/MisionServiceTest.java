package com.ecopeques.ecopeques_backend.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecopeques.ecopeques_backend.dto.MisionResponse;
import com.ecopeques.ecopeques_backend.model.CategoriaMision;
import com.ecopeques.ecopeques_backend.model.Mision;
import com.ecopeques.ecopeques_backend.repository.MisionRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MisionService")
class MisionServiceTest {

    @Mock
    private MisionRepository misionRepository;

    private MisionService misionService;

    @BeforeEach
    void setUp() {
        misionService = new MisionService(misionRepository);
    }

    @Nested
    @DisplayName("obtenerMisionesActivas sin categoría")
    class SinCategoria {

        @Test
        @DisplayName("usa el repositorio general y conserva orden y mapping completo")
        void usaRepositorioGeneralYMapeaEnOrden() {
            Mision agua = crearMision(
                    1L, "Cerrar la llave", "Cierra la llave mientras te cepillas", CategoriaMision.AGUA, 10
            );
            Mision energia = crearMision(
                    2L, "Apagar luces", "Apaga las luces al salir", CategoriaMision.ENERGIA, 15
            );
            when(misionRepository.findByActivaTrueOrderByIdAsc()).thenReturn(List.of(agua, energia));

            List<MisionResponse> response = misionService.obtenerMisionesActivas(null);

            verify(misionRepository).findByActivaTrueOrderByIdAsc();
            verify(misionRepository, never()).findByCategoriaAndActivaTrueOrderByIdAsc(any());
            assertAll(
                    () -> assertFalse(response.isEmpty()),
                    () -> assertEquals(List.of(1L, 2L), response.stream().map(MisionResponse::id).toList()),
                    () -> assertEquals("Cerrar la llave", response.get(0).titulo()),
                    () -> assertEquals("Cierra la llave mientras te cepillas", response.get(0).descripcion()),
                    () -> assertEquals(CategoriaMision.AGUA, response.get(0).categoria()),
                    () -> assertEquals(10, response.get(0).semillasRecompensa()),
                    () -> assertEquals("Apagar luces", response.get(1).titulo()),
                    () -> assertEquals("Apaga las luces al salir", response.get(1).descripcion()),
                    () -> assertEquals(CategoriaMision.ENERGIA, response.get(1).categoria()),
                    () -> assertEquals(15, response.get(1).semillasRecompensa())
            );
        }
    }

    @Nested
    @DisplayName("obtenerMisionesActivas por categoría")
    class PorCategoria {

        @ParameterizedTest(name = "filtra por {0}")
        @EnumSource(CategoriaMision.class)
        @DisplayName("usa el repositorio filtrado para cada categoría")
        void filtraTodasLasCategorias(CategoriaMision categoria) {
            Mision mision = crearMision(
                    3L,
                    "Misión " + categoria,
                    "Descripción " + categoria,
                    categoria,
                    20
            );
            when(misionRepository.findByCategoriaAndActivaTrueOrderByIdAsc(categoria))
                    .thenReturn(List.of(mision));

            List<MisionResponse> response = misionService.obtenerMisionesActivas(categoria);

            verify(misionRepository).findByCategoriaAndActivaTrueOrderByIdAsc(categoria);
            verify(misionRepository, never()).findByActivaTrueOrderByIdAsc();
            assertAll(
                    () -> assertEquals(1, response.size()),
                    () -> assertEquals(3L, response.get(0).id()),
                    () -> assertEquals("Misión " + categoria, response.get(0).titulo()),
                    () -> assertEquals("Descripción " + categoria, response.get(0).descripcion()),
                    () -> assertEquals(categoria, response.get(0).categoria()),
                    () -> assertEquals(20, response.get(0).semillasRecompensa())
            );
        }

        @Test
        @DisplayName("retorna una lista vacía cuando no hay misiones de la categoría")
        void retornaListaVacia() {
            when(misionRepository.findByCategoriaAndActivaTrueOrderByIdAsc(CategoriaMision.BIODIVERSIDAD))
                    .thenReturn(List.of());

            List<MisionResponse> response = misionService.obtenerMisionesActivas(CategoriaMision.BIODIVERSIDAD);

            assertTrue(response.isEmpty());
            verify(misionRepository)
                    .findByCategoriaAndActivaTrueOrderByIdAsc(CategoriaMision.BIODIVERSIDAD);
            verify(misionRepository, never()).findByActivaTrueOrderByIdAsc();
        }
    }

    private Mision crearMision(
            Long id,
            String titulo,
            String descripcion,
            CategoriaMision categoria,
            Integer semillasRecompensa
    ) {
        return Mision.builder()
                .id(id)
                .titulo(titulo)
                .descripcion(descripcion)
                .categoria(categoria)
                .semillasRecompensa(semillasRecompensa)
                .activa(true)
                .build();
    }
}
