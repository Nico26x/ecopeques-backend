package com.ecopeques.ecopeques_backend.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecopeques.ecopeques_backend.dto.NinoRequest;
import com.ecopeques.ecopeques_backend.dto.NinoResponse;
import com.ecopeques.ecopeques_backend.model.Nino;
import com.ecopeques.ecopeques_backend.model.Rol;
import com.ecopeques.ecopeques_backend.model.Usuario;
import com.ecopeques.ecopeques_backend.repository.NinoRepository;
import com.ecopeques.ecopeques_backend.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("NinoService")
class NinoServiceTest {

    @Mock
    private NinoRepository ninoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private NinoService ninoService;

    @BeforeEach
    void setUp() {
        ninoService = new NinoService(ninoRepository, usuarioRepository);
    }

    @Nested
    @DisplayName("crearNino")
    class CrearNino {

        @Test
        @DisplayName("normaliza los datos y mapea todos los campos")
        void normalizaDatosYMapeaRespuestaCompleta() {
            Usuario tutor = crearTutor(1L, "tutor@email.com");
            NinoRequest request = new NinoRequest(" Lucas ", " avatar-1.png ", " Grupo A ");
            when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
            when(ninoRepository.save(any(Nino.class))).thenAnswer(invocation -> {
                Nino nino = invocation.getArgument(0);
                nino.setId(10L);
                return nino;
            });

            NinoResponse response = ninoService.crearNino(request, " TUTOR@EMAIL.COM ");

            ArgumentCaptor<Nino> ninoCaptor = ArgumentCaptor.forClass(Nino.class);
            verify(usuarioRepository).findByEmail("tutor@email.com");
            verify(ninoRepository).save(ninoCaptor.capture());
            Nino ninoGuardado = ninoCaptor.getValue();
            assertAll(
                    () -> assertEquals("Lucas", ninoGuardado.getNombre()),
                    () -> assertEquals("avatar-1.png", ninoGuardado.getAvatar()),
                    () -> assertEquals("Grupo A", ninoGuardado.getGrupo()),
                    () -> assertEquals(0, ninoGuardado.getTotalSemillas()),
                    () -> assertEquals(tutor, ninoGuardado.getTutor()),
                    () -> assertEquals(10L, response.id()),
                    () -> assertEquals("Lucas", response.nombre()),
                    () -> assertEquals(0, response.totalSemillas()),
                    () -> assertEquals("avatar-1.png", response.avatar()),
                    () -> assertEquals("Grupo A", response.grupo()),
                    () -> assertEquals(1L, response.tutorId())
            );
        }

        @ParameterizedTest(name = "convierte opcionales ''{0}'' a null")
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("convierte opcionales null o blancos a null")
        void convierteOpcionalesVaciosANull(String opcional) {
            Usuario tutor = crearTutor(1L, "tutor@email.com");
            NinoRequest request = new NinoRequest("Lucas", opcional, opcional);
            when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
            when(ninoRepository.save(any(Nino.class))).thenAnswer(invocation -> invocation.getArgument(0));

            NinoResponse response = ninoService.crearNino(request, "tutor@email.com");

            ArgumentCaptor<Nino> ninoCaptor = ArgumentCaptor.forClass(Nino.class);
            verify(ninoRepository).save(ninoCaptor.capture());
            assertAll(
                    () -> assertNull(ninoCaptor.getValue().getAvatar()),
                    () -> assertNull(ninoCaptor.getValue().getGrupo()),
                    () -> assertNull(response.avatar()),
                    () -> assertNull(response.grupo())
            );
        }

        @Test
        @DisplayName("no guarda si el usuario no existe")
        void noGuardaSiUsuarioNoExiste() {
            NinoRequest request = new NinoRequest("Lucas", "avatar-1.png", "Grupo A");
            when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.empty());

            UsernameNotFoundException exception = assertThrows(
                    UsernameNotFoundException.class,
                    () -> ninoService.crearNino(request, " TUTOR@EMAIL.COM ")
            );

            assertEquals("Usuario no encontrado", exception.getMessage());
            verify(usuarioRepository).findByEmail("tutor@email.com");
            verify(ninoRepository, never()).save(any(Nino.class));
        }
    }

    @Nested
    @DisplayName("obtenerNinosPorTutor")
    class ObtenerNinosPorTutor {

        @Test
        @DisplayName("normaliza el usuario y conserva orden y mapping completo")
        void normalizaUsuarioYMapeaListaCompletaEnOrden() {
            Usuario tutor = crearTutor(1L, "tutor@email.com");
            Nino lucas = crearNino(10L, "Lucas", "avatar-1.png", "Grupo A", 25, tutor);
            Nino sofia = crearNino(11L, "Sofia", null, null, 7, tutor);
            when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
            when(ninoRepository.findByTutorIdOrderByIdAsc(1L)).thenReturn(List.of(lucas, sofia));

            List<NinoResponse> response = ninoService.obtenerNinosPorTutor(1L, " TUTOR@EMAIL.COM ");

            verify(usuarioRepository).findByEmail("tutor@email.com");
            verify(ninoRepository).findByTutorIdOrderByIdAsc(1L);
            assertAll(
                    () -> assertFalse(response.isEmpty()),
                    () -> assertEquals(List.of(10L, 11L), response.stream().map(NinoResponse::id).toList()),
                    () -> assertEquals("Lucas", response.get(0).nombre()),
                    () -> assertEquals(25, response.get(0).totalSemillas()),
                    () -> assertEquals("avatar-1.png", response.get(0).avatar()),
                    () -> assertEquals("Grupo A", response.get(0).grupo()),
                    () -> assertEquals(1L, response.get(0).tutorId()),
                    () -> assertEquals("Sofia", response.get(1).nombre()),
                    () -> assertEquals(7, response.get(1).totalSemillas()),
                    () -> assertNull(response.get(1).avatar()),
                    () -> assertNull(response.get(1).grupo()),
                    () -> assertEquals(1L, response.get(1).tutorId())
            );
        }

        @Test
        @DisplayName("retorna una lista vacía cuando el tutor no tiene niños")
        void retornaListaVacia() {
            Usuario tutor = crearTutor(1L, "tutor@email.com");
            when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
            when(ninoRepository.findByTutorIdOrderByIdAsc(1L)).thenReturn(List.of());

            List<NinoResponse> response = ninoService.obtenerNinosPorTutor(1L, "tutor@email.com");

            assertTrue(response.isEmpty());
            verify(ninoRepository).findByTutorIdOrderByIdAsc(1L);
        }

        @Test
        @DisplayName("no consulta niños si el usuario autenticado no existe")
        void noConsultaNinosSiUsuarioNoExiste() {
            when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.empty());

            UsernameNotFoundException exception = assertThrows(
                    UsernameNotFoundException.class,
                    () -> ninoService.obtenerNinosPorTutor(1L, " TUTOR@EMAIL.COM ")
            );

            assertEquals("Usuario no encontrado", exception.getMessage());
            verify(usuarioRepository).findByEmail("tutor@email.com");
            verify(ninoRepository, never()).findByTutorIdOrderByIdAsc(any());
        }

        @Test
        @DisplayName("no consulta niños si el tutor autenticado no coincide")
        void noConsultaNinosSiTutorNoCoincide() {
            Usuario tutor = crearTutor(1L, "tutor@email.com");
            when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));

            assertThrows(
                    AccessDeniedException.class,
                    () -> ninoService.obtenerNinosPorTutor(2L, "tutor@email.com")
            );

            verify(ninoRepository, never()).findByTutorIdOrderByIdAsc(any());
        }
    }

    private Usuario crearTutor(Long id, String email) {
        return Usuario.builder()
                .id(id)
                .nombre("Tutor")
                .apellido("EcoPeques")
                .email(email)
                .password("password-encriptado")
                .rol(Rol.PADRE)
                .build();
    }

    private Nino crearNino(
            Long id,
            String nombre,
            String avatar,
            String grupo,
            Integer totalSemillas,
            Usuario tutor
    ) {
        return Nino.builder()
                .id(id)
                .nombre(nombre)
                .avatar(avatar)
                .grupo(grupo)
                .totalSemillas(totalSemillas)
                .tutor(tutor)
                .build();
    }
}
