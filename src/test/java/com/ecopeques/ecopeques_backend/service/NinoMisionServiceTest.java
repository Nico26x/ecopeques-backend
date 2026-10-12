package com.ecopeques.ecopeques_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecopeques.ecopeques_backend.dto.AsignarMisionRequest;
import com.ecopeques.ecopeques_backend.dto.MisionAsignadaResponse;
import com.ecopeques.ecopeques_backend.exception.LimiteMisionesAlcanzadoException;
import com.ecopeques.ecopeques_backend.exception.MisionNotFoundException;
import com.ecopeques.ecopeques_backend.exception.MisionYaAsignadaException;
import com.ecopeques.ecopeques_backend.exception.NinoNotFoundException;
import com.ecopeques.ecopeques_backend.model.CategoriaMision;
import com.ecopeques.ecopeques_backend.model.EstadoMisionAsignada;
import com.ecopeques.ecopeques_backend.model.Mision;
import com.ecopeques.ecopeques_backend.model.Nino;
import com.ecopeques.ecopeques_backend.model.NinoMision;
import com.ecopeques.ecopeques_backend.model.Rol;
import com.ecopeques.ecopeques_backend.model.Usuario;
import com.ecopeques.ecopeques_backend.repository.MisionRepository;
import com.ecopeques.ecopeques_backend.repository.NinoMisionRepository;
import com.ecopeques.ecopeques_backend.repository.NinoRepository;
import com.ecopeques.ecopeques_backend.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class NinoMisionServiceTest {

    @Mock
    private NinoRepository ninoRepository;

    @Mock
    private MisionRepository misionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private NinoMisionRepository ninoMisionRepository;

    private NinoMisionService ninoMisionService;

    @BeforeEach
    void setUp() {
        ninoMisionService = new NinoMisionService(
                ninoRepository,
                misionRepository,
                usuarioRepository,
                ninoMisionRepository
        );
    }

    @Test
    void asignarMision_CuandoDatosValidos_DeberiaCrearAsignacionEnProgreso() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino nino = crearNino(10L, tutor);
        Mision mision = crearMision(20L, true);
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L);
        LocalDateTime fechaAsignacion = LocalDateTime.now();

        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoRepository.findById(10L)).thenReturn(Optional.of(nino));
        when(misionRepository.findById(20L)).thenReturn(Optional.of(mision));
        when(ninoMisionRepository.existsByNinoIdAndMisionIdAndEstado(10L, 20L, EstadoMisionAsignada.EN_PROGRESO))
                .thenReturn(false);
        when(ninoMisionRepository.countByNinoIdAndEstado(10L, EstadoMisionAsignada.EN_PROGRESO)).thenReturn(2L);
        when(ninoMisionRepository.save(any(NinoMision.class))).thenAnswer(invocation -> {
            NinoMision asignacion = invocation.getArgument(0);
            asignacion.setId(100L);
            asignacion.setFechaAsignacion(fechaAsignacion);
            return asignacion;
        });

        MisionAsignadaResponse response = ninoMisionService.asignarMision(request, " TUTOR@EMAIL.COM ");

        ArgumentCaptor<NinoMision> asignacionCaptor = ArgumentCaptor.forClass(NinoMision.class);
        verify(ninoMisionRepository).save(asignacionCaptor.capture());

        NinoMision asignacionGuardada = asignacionCaptor.getValue();
        assertEquals(nino, asignacionGuardada.getNino());
        assertEquals(mision, asignacionGuardada.getMision());
        assertEquals(EstadoMisionAsignada.EN_PROGRESO, asignacionGuardada.getEstado());

        assertEquals(100L, response.id());
        assertEquals(10L, response.ninoId());
        assertEquals(20L, response.misionId());
        assertEquals("Cerrar la llave", response.tituloMision());
        assertEquals(CategoriaMision.AGUA, response.categoria());
        assertEquals(10, response.semillasRecompensa());
        assertEquals(EstadoMisionAsignada.EN_PROGRESO, response.estado());
        assertEquals(fechaAsignacion, response.fechaAsignacion());
    }

    @Test
    void asignarMision_CuandoNinoNoExiste_DeberiaLanzarNinoNotFoundException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L);

        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NinoNotFoundException.class, () -> ninoMisionService.asignarMision(request, "tutor@email.com"));

        verify(misionRepository, never()).findById(20L);
        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void asignarMision_CuandoMisionNoExiste_DeberiaLanzarMisionNotFoundException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino nino = crearNino(10L, tutor);
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L);

        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoRepository.findById(10L)).thenReturn(Optional.of(nino));
        when(misionRepository.findById(20L)).thenReturn(Optional.empty());

        assertThrows(MisionNotFoundException.class, () -> ninoMisionService.asignarMision(request, "tutor@email.com"));

        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void asignarMision_CuandoNinoNoPerteneceAlTutor_DeberiaLanzarAccessDeniedException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Usuario otroTutor = crearTutor(2L, "otro@email.com");
        Nino nino = crearNino(10L, otroTutor);
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L);

        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoRepository.findById(10L)).thenReturn(Optional.of(nino));

        assertThrows(AccessDeniedException.class, () -> ninoMisionService.asignarMision(request, "tutor@email.com"));

        verify(misionRepository, never()).findById(20L);
        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void asignarMision_CuandoMisionYaEstaEnProgreso_DeberiaLanzarMisionYaAsignadaException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino nino = crearNino(10L, tutor);
        Mision mision = crearMision(20L, true);
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L);

        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoRepository.findById(10L)).thenReturn(Optional.of(nino));
        when(misionRepository.findById(20L)).thenReturn(Optional.of(mision));
        when(ninoMisionRepository.existsByNinoIdAndMisionIdAndEstado(10L, 20L, EstadoMisionAsignada.EN_PROGRESO))
                .thenReturn(true);

        assertThrows(MisionYaAsignadaException.class, () -> ninoMisionService.asignarMision(request, "tutor@email.com"));

        verify(ninoMisionRepository, never()).countByNinoIdAndEstado(10L, EstadoMisionAsignada.EN_PROGRESO);
        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void asignarMision_CuandoNinoTieneTresMisionesActivas_DeberiaLanzarLimiteMisionesAlcanzadoException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino nino = crearNino(10L, tutor);
        Mision mision = crearMision(20L, true);
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L);

        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoRepository.findById(10L)).thenReturn(Optional.of(nino));
        when(misionRepository.findById(20L)).thenReturn(Optional.of(mision));
        when(ninoMisionRepository.existsByNinoIdAndMisionIdAndEstado(10L, 20L, EstadoMisionAsignada.EN_PROGRESO))
                .thenReturn(false);
        when(ninoMisionRepository.countByNinoIdAndEstado(10L, EstadoMisionAsignada.EN_PROGRESO)).thenReturn(3L);

        assertThrows(
                LimiteMisionesAlcanzadoException.class,
                () -> ninoMisionService.asignarMision(request, "tutor@email.com")
        );

        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
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

    private Nino crearNino(Long id, Usuario tutor) {
        return Nino.builder()
                .id(id)
                .nombre("Lucas")
                .avatar("avatar.png")
                .grupo("Grupo A")
                .totalSemillas(0)
                .tutor(tutor)
                .build();
    }

    private Mision crearMision(Long id, boolean activa) {
        return Mision.builder()
                .id(id)
                .titulo("Cerrar la llave")
                .descripcion("Ahorra agua cerrando la llave mientras te lavas los dientes.")
                .categoria(CategoriaMision.AGUA)
                .semillasRecompensa(10)
                .activa(activa)
                .build();
    }
}
