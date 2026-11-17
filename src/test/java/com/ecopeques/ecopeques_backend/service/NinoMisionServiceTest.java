package com.ecopeques.ecopeques_backend.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.ecopeques.ecopeques_backend.model.Mision;
import com.ecopeques.ecopeques_backend.model.Nino;
import com.ecopeques.ecopeques_backend.model.NinoMision;
import com.ecopeques.ecopeques_backend.model.Rol;
import com.ecopeques.ecopeques_backend.model.Usuario;
import com.ecopeques.ecopeques_backend.repository.MisionRepository;
import com.ecopeques.ecopeques_backend.repository.NinoMisionRepository;
import com.ecopeques.ecopeques_backend.repository.NinoRepository;
import com.ecopeques.ecopeques_backend.repository.UsuarioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class NinoMisionServiceTest {

    private static final int LONGITUD_MAXIMA_EVIDENCIA = 2_800_000;

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

    @Nested
    @DisplayName("HU-05 Asignación")
    class Asignacion {

    @Test
    void asignarMision_CuandoDatosValidos_DeberiaCrearAsignacionEnProgreso() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino nino = crearNino(10L, tutor);
        Mision mision = crearMision(20L, true);
        LocalDate fechaLimite = LocalDate.now().plusDays(7);
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L, fechaLimite);
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
        assertAll(
                () -> assertEquals(nino, asignacionGuardada.getNino()),
                () -> assertEquals(mision, asignacionGuardada.getMision()),
                () -> assertEquals(EstadoMisionAsignada.EN_PROGRESO, asignacionGuardada.getEstado()),
                () -> assertEquals(fechaLimite, asignacionGuardada.getFechaLimite()),
                () -> assertEquals(100L, response.id()),
                () -> assertEquals(10L, response.ninoId()),
                () -> assertEquals(20L, response.misionId()),
                () -> assertEquals("Cerrar la llave", response.tituloMision()),
                () -> assertEquals(CategoriaMision.AGUA, response.categoria()),
                () -> assertEquals(10, response.semillasRecompensa()),
                () -> assertEquals(EstadoMisionAsignada.EN_PROGRESO, response.estado()),
                () -> assertEquals(fechaAsignacion, response.fechaAsignacion()),
                () -> assertEquals(fechaLimite, response.fechaLimite()),
                () -> assertNull(response.evidencia()),
                () -> assertFalse(response.acreditada())
        );
    }

    @Test
    void asignarMision_CuandoNinoNoExiste_DeberiaLanzarNinoNotFoundException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L, null);

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
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L, null);

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
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L, null);

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
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L, null);

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
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L, null);

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

    @ParameterizedTest
    @ValueSource(booleans = {false})
    void asignarMision_CuandoMisionEstaInactiva_DeberiaLanzarMisionNotFoundException(boolean activa) {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino nino = crearNino(10L, tutor);
        Mision mision = crearMision(20L, activa);
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L, null);
        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoRepository.findById(10L)).thenReturn(Optional.of(nino));
        when(misionRepository.findById(20L)).thenReturn(Optional.of(mision));

        assertThrows(MisionNotFoundException.class, () -> ninoMisionService.asignarMision(request, "tutor@email.com"));

        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void asignarMision_CuandoActivaEsNull_DeberiaLanzarMisionNotFoundException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino nino = crearNino(10L, tutor);
        Mision mision = crearMision(20L, true);
        mision.setActiva(null);
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L, null);
        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoRepository.findById(10L)).thenReturn(Optional.of(nino));
        when(misionRepository.findById(20L)).thenReturn(Optional.of(mision));

        assertThrows(MisionNotFoundException.class, () -> ninoMisionService.asignarMision(request, "tutor@email.com"));

        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void asignarMision_CuandoNinoTieneMasDeTresMisionesActivas_DeberiaLanzarLimiteMisionesAlcanzadoException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino nino = crearNino(10L, tutor);
        Mision mision = crearMision(20L, true);
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L, null);
        configurarAsignacionValida(tutor, nino, mision);
        when(ninoMisionRepository.countByNinoIdAndEstado(10L, EstadoMisionAsignada.EN_PROGRESO)).thenReturn(4L);

        assertThrows(
                LimiteMisionesAlcanzadoException.class,
                () -> ninoMisionService.asignarMision(request, "tutor@email.com")
        );

        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void asignarMision_ConFechaLimiteNull_DeberiaGuardarLaAsignacionSinFechaLimite() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino nino = crearNino(10L, tutor);
        Mision mision = crearMision(20L, true);
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L, null);
        configurarAsignacionValida(tutor, nino, mision);
        when(ninoMisionRepository.countByNinoIdAndEstado(10L, EstadoMisionAsignada.EN_PROGRESO)).thenReturn(0L);
        when(ninoMisionRepository.save(any(NinoMision.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MisionAsignadaResponse response = ninoMisionService.asignarMision(request, "tutor@email.com");

        ArgumentCaptor<NinoMision> captor = ArgumentCaptor.forClass(NinoMision.class);
        verify(ninoMisionRepository).save(captor.capture());
        assertAll(
                () -> assertNull(captor.getValue().getFechaLimite()),
                () -> assertNull(response.fechaLimite())
        );
    }

    @Test
    void asignarMision_CuandoUsuarioAutenticadoNoExiste_DeberiaLanzarUsernameNotFoundException() {
        when(usuarioRepository.findByEmail("ausente@email.com")).thenReturn(Optional.empty());
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L, null);

        assertThrows(
                UsernameNotFoundException.class,
                () -> ninoMisionService.asignarMision(request, " AUSENTE@EMAIL.COM ")
        );

        verify(ninoRepository, never()).findById(any(Long.class));
        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }
    }

    @Nested
    @DisplayName("HU-06 Confirmación")
    class Confirmacion {

    @Test
    void confirmarMision_SinEvidencia_DeberiaCompletarSinAcreditarSemillas() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino nino = crearNino(10L, tutor);
        nino.setTotalSemillas(25);
        NinoMision asignacion = crearAsignacion(100L, nino, EstadoMisionAsignada.EN_PROGRESO);

        configurarConfirmacion(tutor, asignacion);

        MisionAsignadaResponse response = ninoMisionService.confirmarMision(
                100L,
                null,
                " TUTOR@EMAIL.COM "
        );

        assertAll(
                () -> assertEquals(EstadoMisionAsignada.COMPLETADA, asignacion.getEstado()),
                () -> assertNotNull(asignacion.getFechaCompletado()),
                () -> assertNull(asignacion.getEvidencia()),
                () -> assertFalse(asignacion.getAcreditada()),
                () -> assertEquals(25, nino.getTotalSemillas()),
                () -> assertEquals(EstadoMisionAsignada.COMPLETADA, response.estado()),
                () -> assertFalse(response.acreditada())
        );
        verify(ninoMisionRepository).save(asignacion);
    }

    @Test
    void confirmarMision_ConUrlValida_DeberiaGuardarEvidenciaNormalizada() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        NinoMision asignacion = crearAsignacion(
                100L,
                crearNino(10L, tutor),
                EstadoMisionAsignada.EN_PROGRESO
        );
        configurarConfirmacion(tutor, asignacion);

        MisionAsignadaResponse response = ninoMisionService.confirmarMision(
                100L,
                new ConfirmarMisionRequest("  https://example.com/evidencia.jpg  "),
                "tutor@email.com"
        );

        assertAll(
                () -> assertEquals(EstadoMisionAsignada.COMPLETADA, asignacion.getEstado()),
                () -> assertNotNull(asignacion.getFechaCompletado()),
                () -> assertEquals("https://example.com/evidencia.jpg", asignacion.getEvidencia()),
                () -> assertFalse(asignacion.getAcreditada()),
                () -> assertEquals(EstadoMisionAsignada.COMPLETADA, response.estado()),
                () -> assertEquals(asignacion.getFechaCompletado(), response.fechaCompletado()),
                () -> assertEquals("https://example.com/evidencia.jpg", response.evidencia()),
                () -> assertFalse(response.acreditada())
        );
        verify(ninoMisionRepository).save(asignacion);
    }

    @Test
    void confirmarMision_ConImagenBase64Valida_DeberiaGuardarEvidencia() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        NinoMision asignacion = crearAsignacion(
                100L,
                crearNino(10L, tutor),
                EstadoMisionAsignada.EN_PROGRESO
        );
        String evidencia = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB";
        configurarConfirmacion(tutor, asignacion);

        MisionAsignadaResponse response = ninoMisionService.confirmarMision(
                100L,
                new ConfirmarMisionRequest(evidencia),
                "tutor@email.com"
        );

        assertAll(
                () -> assertEquals(EstadoMisionAsignada.COMPLETADA, asignacion.getEstado()),
                () -> assertNotNull(asignacion.getFechaCompletado()),
                () -> assertEquals(evidencia, asignacion.getEvidencia()),
                () -> assertFalse(asignacion.getAcreditada()),
                () -> assertEquals(EstadoMisionAsignada.COMPLETADA, response.estado()),
                () -> assertEquals(asignacion.getFechaCompletado(), response.fechaCompletado()),
                () -> assertEquals(evidencia, response.evidencia()),
                () -> assertFalse(response.acreditada())
        );
        verify(ninoMisionRepository).save(asignacion);
    }

    @Test
    void confirmarMision_CuandoAsignacionNoExiste_DeberiaLanzarNinoMisionNotFoundException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoMisionRepository.findByIdForUpdate(100L)).thenReturn(Optional.empty());

        assertThrows(
                NinoMisionNotFoundException.class,
                () -> ninoMisionService.confirmarMision(100L, null, "tutor@email.com")
        );

        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void confirmarMision_CuandoPerteneceAOtroTutor_DeberiaLanzarAccessDeniedException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Usuario otroTutor = crearTutor(2L, "otro@email.com");
        NinoMision asignacion = crearAsignacion(
                100L,
                crearNino(10L, otroTutor),
                EstadoMisionAsignada.EN_PROGRESO
        );

        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoMisionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(asignacion));

        assertThrows(
                AccessDeniedException.class,
                () -> ninoMisionService.confirmarMision(100L, null, "tutor@email.com")
        );

        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void confirmarMision_CuandoYaEstaCompletada_DeberiaLanzarMisionNoConfirmableException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        NinoMision asignacion = crearAsignacion(
                100L,
                crearNino(10L, tutor),
                EstadoMisionAsignada.COMPLETADA
        );

        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoMisionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(asignacion));

        assertThrows(
                MisionNoConfirmableException.class,
                () -> ninoMisionService.confirmarMision(100L, null, "tutor@email.com")
        );

        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void confirmarMision_ConFormatoDeEvidenciaInvalido_DeberiaLanzarEvidenciaInvalidaException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        NinoMision asignacion = crearAsignacion(
                100L,
                crearNino(10L, tutor),
                EstadoMisionAsignada.EN_PROGRESO
        );
        configurarAsignacionParaConfirmar(tutor, asignacion);

        assertThrows(
                EvidenciaInvalidaException.class,
                () -> ninoMisionService.confirmarMision(
                        100L,
                        new ConfirmarMisionRequest("archivo-local.jpg"),
                        "tutor@email.com"
                )
        );

        assertAsignacionSinMutacion(asignacion);
        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void confirmarMision_ConEvidenciaDemasiadoGrande_DeberiaLanzarEvidenciaInvalidaException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        NinoMision asignacion = crearAsignacion(
                100L,
                crearNino(10L, tutor),
                EstadoMisionAsignada.EN_PROGRESO
        );
        String evidencia = "data:image/png;base64," + "A".repeat(2_800_000);
        configurarAsignacionParaConfirmar(tutor, asignacion);

        assertThrows(
                EvidenciaInvalidaException.class,
                () -> ninoMisionService.confirmarMision(
                        100L,
                        new ConfirmarMisionRequest(evidencia),
                        "tutor@email.com"
                )
        );

        assertAsignacionSinMutacion(asignacion);
        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void confirmarMision_ConRequestNoNuloYEvidenciaNull_DeberiaCompletarSinEvidencia() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        NinoMision asignacion = crearAsignacion(100L, crearNino(10L, tutor), EstadoMisionAsignada.EN_PROGRESO);
        configurarConfirmacion(tutor, asignacion);

        MisionAsignadaResponse response = ninoMisionService.confirmarMision(
                100L,
                new ConfirmarMisionRequest(null),
                "tutor@email.com"
        );

        assertAll(
                () -> assertEquals(EstadoMisionAsignada.COMPLETADA, asignacion.getEstado()),
                () -> assertNotNull(asignacion.getFechaCompletado()),
                () -> assertNull(asignacion.getEvidencia()),
                () -> assertFalse(asignacion.getAcreditada()),
                () -> assertNull(response.evidencia())
        );
        verify(ninoMisionRepository).save(asignacion);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   \t  "})
    void confirmarMision_ConEvidenciaVaciaOWhitespace_DeberiaCompletarSinEvidencia(String evidencia) {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        NinoMision asignacion = crearAsignacion(100L, crearNino(10L, tutor), EstadoMisionAsignada.EN_PROGRESO);
        configurarConfirmacion(tutor, asignacion);

        MisionAsignadaResponse response = ninoMisionService.confirmarMision(
                100L,
                new ConfirmarMisionRequest(evidencia),
                "tutor@email.com"
        );

        assertAll(
                () -> assertEquals(EstadoMisionAsignada.COMPLETADA, asignacion.getEstado()),
                () -> assertNotNull(asignacion.getFechaCompletado()),
                () -> assertNull(asignacion.getEvidencia()),
                () -> assertFalse(asignacion.getAcreditada()),
                () -> assertNull(response.evidencia())
        );
        verify(ninoMisionRepository).save(asignacion);
    }

    @Test
    void confirmarMision_ConUrlHttpValida_DeberiaGuardarEvidencia() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        NinoMision asignacion = crearAsignacion(100L, crearNino(10L, tutor), EstadoMisionAsignada.EN_PROGRESO);
        configurarConfirmacion(tutor, asignacion);
        String evidencia = "http://example.com/evidencia.jpg";

        MisionAsignadaResponse response = ninoMisionService.confirmarMision(
                100L,
                new ConfirmarMisionRequest(evidencia),
                "tutor@email.com"
        );

        assertAll(
                () -> assertEquals(EstadoMisionAsignada.COMPLETADA, asignacion.getEstado()),
                () -> assertNotNull(asignacion.getFechaCompletado()),
                () -> assertEquals(evidencia, asignacion.getEvidencia()),
                () -> assertEquals(evidencia, response.evidencia())
        );
        verify(ninoMisionRepository).save(asignacion);
    }

    @Test
    void confirmarMision_ConEvidenciaEnLimiteExacto_DeberiaGuardarEvidencia() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        NinoMision asignacion = crearAsignacion(100L, crearNino(10L, tutor), EstadoMisionAsignada.EN_PROGRESO);
        configurarConfirmacion(tutor, asignacion);
        String prefijo = "data:image/png;base64,";
        String evidencia = prefijo + "A".repeat(LONGITUD_MAXIMA_EVIDENCIA - prefijo.length());

        MisionAsignadaResponse response = ninoMisionService.confirmarMision(
                100L,
                new ConfirmarMisionRequest(evidencia),
                "tutor@email.com"
        );

        assertAll(
                () -> assertEquals(LONGITUD_MAXIMA_EVIDENCIA, asignacion.getEvidencia().length()),
                () -> assertEquals(evidencia, response.evidencia()),
                () -> assertEquals(EstadoMisionAsignada.COMPLETADA, asignacion.getEstado()),
                () -> assertNotNull(asignacion.getFechaCompletado())
        );
        verify(ninoMisionRepository).save(asignacion);
    }

    @Test
    void confirmarMision_ConUnCaracterSobreElLimite_DeberiaRechazarSinMutar() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        NinoMision asignacion = crearAsignacion(100L, crearNino(10L, tutor), EstadoMisionAsignada.EN_PROGRESO);
        configurarAsignacionParaConfirmar(tutor, asignacion);
        String prefijo = "data:image/png;base64,";
        String evidencia = prefijo + "A".repeat(LONGITUD_MAXIMA_EVIDENCIA + 1 - prefijo.length());

        assertThrows(
                EvidenciaInvalidaException.class,
                () -> ninoMisionService.confirmarMision(
                        100L,
                        new ConfirmarMisionRequest(evidencia),
                        "tutor@email.com"
                )
        );

        assertAsignacionSinMutacion(asignacion);
        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "HTTP://example.com/evidencia.jpg",
            "HTTPS://example.com/evidencia.jpg",
            "DATA:image/png;base64,AAAA"
    })
    void confirmarMision_ConPrefijoEnMayusculas_DeberiaRechazarSinMutar(String evidencia) {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        NinoMision asignacion = crearAsignacion(100L, crearNino(10L, tutor), EstadoMisionAsignada.EN_PROGRESO);
        configurarAsignacionParaConfirmar(tutor, asignacion);

        assertThrows(
                EvidenciaInvalidaException.class,
                () -> ninoMisionService.confirmarMision(
                        100L,
                        new ConfirmarMisionRequest(evidencia),
                        "tutor@email.com"
                )
        );

        assertAsignacionSinMutacion(asignacion);
        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void confirmarMision_CuandoUsuarioAutenticadoNoExiste_DeberiaLanzarUsernameNotFoundException() {
        when(usuarioRepository.findByEmail("ausente@email.com")).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> ninoMisionService.confirmarMision(100L, null, " AUSENTE@EMAIL.COM ")
        );

        verify(ninoMisionRepository, never()).findByIdForUpdate(any(Long.class));
        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }
    }

    @Nested
    @DisplayName("HU-07 Acreditación")
    class Acreditacion {

    @Test
    void acreditarMision_CuandoEstaCompletada_DeberiaSumarSemillasYMarcarAcreditada() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino nino = crearNino(10L, tutor);
        nino.setTotalSemillas(25);
        NinoMision asignacion = crearAsignacion(100L, nino, EstadoMisionAsignada.COMPLETADA);
        asignacion.getMision().setSemillasRecompensa(10);
        asignacion.getMision().setPuntosOtorgados(99);

        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoMisionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(asignacion));
        when(ninoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(nino));

        MisionAcreditadaResponse response = ninoMisionService.acreditarMision(
                100L,
                " TUTOR@EMAIL.COM "
        );

        assertAll(
                () -> assertEquals(35, nino.getTotalSemillas()),
                () -> assertTrue(asignacion.getAcreditada()),
                () -> assertEquals(EstadoMisionAsignada.COMPLETADA, asignacion.getEstado()),
                () -> assertEquals(100L, response.id()),
                () -> assertEquals(10L, response.ninoId()),
                () -> assertEquals(20L, response.misionId()),
                () -> assertEquals(10, response.semillasAcreditadas()),
                () -> assertEquals(35, response.totalSemillas()),
                () -> assertTrue(response.acreditada())
        );
        verify(ninoMisionRepository).findByIdForUpdate(100L);
        verify(ninoRepository).findByIdForUpdate(10L);
        verify(ninoRepository).save(nino);
        verify(ninoMisionRepository).save(asignacion);
    }

    @Test
    void acreditarMision_CuandoAsignacionNoExiste_DeberiaLanzarNinoMisionNotFoundException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoMisionRepository.findByIdForUpdate(100L)).thenReturn(Optional.empty());

        assertThrows(
                NinoMisionNotFoundException.class,
                () -> ninoMisionService.acreditarMision(100L, "tutor@email.com")
        );

        verify(ninoRepository, never()).findByIdForUpdate(any(Long.class));
        verify(ninoRepository, never()).save(any(Nino.class));
        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void acreditarMision_CuandoPerteneceAOtroTutor_DeberiaLanzarAccessDeniedException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Usuario otroTutor = crearTutor(2L, "otro@email.com");
        Nino nino = crearNino(10L, otroTutor);
        NinoMision asignacion = crearAsignacion(100L, nino, EstadoMisionAsignada.COMPLETADA);
        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoMisionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(asignacion));

        assertThrows(
                AccessDeniedException.class,
                () -> ninoMisionService.acreditarMision(100L, "tutor@email.com")
        );

        assertAll(
                () -> assertEquals(0, nino.getTotalSemillas()),
                () -> assertFalse(asignacion.getAcreditada())
        );
        verify(ninoRepository, never()).findByIdForUpdate(any(Long.class));
        verify(ninoRepository, never()).save(any(Nino.class));
        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void acreditarMision_CuandoEstaEnProgreso_DeberiaLanzarMisionNoAcreditableException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino nino = crearNino(10L, tutor);
        NinoMision asignacion = crearAsignacion(100L, nino, EstadoMisionAsignada.EN_PROGRESO);
        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoMisionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(asignacion));

        MisionNoAcreditableException exception = assertThrows(
                MisionNoAcreditableException.class,
                () -> ninoMisionService.acreditarMision(100L, "tutor@email.com")
        );

        assertAll(
                () -> assertEquals("La misión debe estar completada antes de acreditarla", exception.getMessage()),
                () -> assertEquals(0, nino.getTotalSemillas()),
                () -> assertFalse(asignacion.getAcreditada())
        );
        verify(ninoRepository, never()).findByIdForUpdate(any(Long.class));
        verify(ninoRepository, never()).save(any(Nino.class));
        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void acreditarMision_CuandoYaFueAcreditada_DeberiaLanzarMisionNoAcreditableException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino nino = crearNino(10L, tutor);
        nino.setTotalSemillas(10);
        NinoMision asignacion = crearAsignacion(100L, nino, EstadoMisionAsignada.COMPLETADA);
        asignacion.setAcreditada(true);
        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoMisionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(asignacion));

        MisionNoAcreditableException exception = assertThrows(
                MisionNoAcreditableException.class,
                () -> ninoMisionService.acreditarMision(100L, "tutor@email.com")
        );

        assertEquals("La misión ya fue acreditada", exception.getMessage());
        assertEquals(10, nino.getTotalSemillas());
        verify(ninoRepository, never()).findByIdForUpdate(any(Long.class));
        verify(ninoRepository, never()).save(any(Nino.class));
        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void acreditarMision_CuandoNinoNoExiste_DeberiaLanzarNinoNotFoundException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino nino = crearNino(10L, tutor);
        NinoMision asignacion = crearAsignacion(100L, nino, EstadoMisionAsignada.COMPLETADA);
        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoMisionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(asignacion));
        when(ninoRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThrows(
                NinoNotFoundException.class,
                () -> ninoMisionService.acreditarMision(100L, "tutor@email.com")
        );

        assertAll(
                () -> assertEquals(0, nino.getTotalSemillas()),
                () -> assertFalse(asignacion.getAcreditada())
        );
        verify(ninoRepository, never()).save(any(Nino.class));
        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void acreditarMision_CuandoAcreditadaEsNull_DeberiaTratarlaComoNoAcreditada() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino nino = crearNino(10L, tutor);
        nino.setTotalSemillas(25);
        NinoMision asignacion = crearAsignacion(100L, nino, EstadoMisionAsignada.COMPLETADA);
        asignacion.setAcreditada(null);
        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoMisionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(asignacion));
        when(ninoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(nino));

        MisionAcreditadaResponse response = ninoMisionService.acreditarMision(100L, "tutor@email.com");

        assertAll(
                () -> assertEquals(35, nino.getTotalSemillas()),
                () -> assertTrue(asignacion.getAcreditada()),
                () -> assertEquals(10, response.semillasAcreditadas()),
                () -> assertEquals(35, response.totalSemillas()),
                () -> assertTrue(response.acreditada())
        );
        verify(ninoRepository).save(nino);
        verify(ninoMisionRepository).save(asignacion);
    }

    @Test
    void acreditarMision_CuandoEstadoEsNull_DeberiaLanzarMisionNoAcreditableException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino nino = crearNino(10L, tutor);
        NinoMision asignacion = crearAsignacion(100L, nino, null);
        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoMisionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(asignacion));

        MisionNoAcreditableException exception = assertThrows(
                MisionNoAcreditableException.class,
                () -> ninoMisionService.acreditarMision(100L, "tutor@email.com")
        );

        assertAll(
                () -> assertEquals("La misión debe estar completada antes de acreditarla", exception.getMessage()),
                () -> assertEquals(0, nino.getTotalSemillas()),
                () -> assertFalse(asignacion.getAcreditada())
        );
        verify(ninoRepository, never()).findByIdForUpdate(any(Long.class));
        verify(ninoRepository, never()).save(any(Nino.class));
        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void acreditarMision_CuandoUsuarioAutenticadoNoExiste_DeberiaLanzarUsernameNotFoundException() {
        when(usuarioRepository.findByEmail("ausente@email.com")).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> ninoMisionService.acreditarMision(100L, " AUSENTE@EMAIL.COM ")
        );

        verify(ninoMisionRepository, never()).findByIdForUpdate(any(Long.class));
        verify(ninoRepository, never()).save(any(Nino.class));
        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }
    }

    @Nested
    @DisplayName("Listado")
    class Listado {

    @Test
    void listarPendientesPorTutor_DeberiaRetornarAsignacionesEnProgreso() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        NinoMision asignacion = crearAsignacion(
                100L,
                crearNino(10L, tutor),
                EstadoMisionAsignada.EN_PROGRESO
        );
        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoMisionRepository.findByNinoTutorIdAndEstadoOrderByFechaAsignacionDesc(
                1L,
                EstadoMisionAsignada.EN_PROGRESO
        )).thenReturn(List.of(asignacion));

        List<MisionAsignadaResponse> response = ninoMisionService.listarPendientesPorTutor("tutor@email.com");

        assertEquals(1, response.size());
        assertEquals(100L, response.getFirst().id());
        assertEquals(EstadoMisionAsignada.EN_PROGRESO, response.getFirst().estado());
    }

    @Test
    void listarPendientesPorTutor_CuandoNoHayPendientes_DeberiaRetornarListaVacia() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoMisionRepository.findByNinoTutorIdAndEstadoOrderByFechaAsignacionDesc(
                1L,
                EstadoMisionAsignada.EN_PROGRESO
        )).thenReturn(List.of());

        List<MisionAsignadaResponse> response = ninoMisionService.listarPendientesPorTutor("tutor@email.com");

        assertTrue(response.isEmpty());
    }

    @Test
    void listarPendientesPorTutor_ConMultiplesAsignaciones_DeberiaMapearTodosLosCampos() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino primerNino = crearNino(10L, tutor);
        Nino segundoNino = crearNino(11L, tutor);
        LocalDateTime primeraFechaAsignacion = LocalDateTime.of(2026, 11, 14, 10, 30);
        LocalDateTime segundaFechaAsignacion = LocalDateTime.of(2026, 11, 13, 9, 15);
        LocalDate primeraFechaLimite = LocalDate.of(2026, 11, 21);
        LocalDate segundaFechaLimite = LocalDate.of(2026, 11, 20);
        NinoMision primera = crearAsignacion(100L, primerNino, EstadoMisionAsignada.EN_PROGRESO);
        primera.setFechaAsignacion(primeraFechaAsignacion);
        primera.setFechaLimite(primeraFechaLimite);
        primera.setEvidencia("http://example.com/primera.jpg");
        primera.setAcreditada(false);
        NinoMision segunda = crearAsignacion(101L, segundoNino, EstadoMisionAsignada.EN_PROGRESO);
        segunda.getMision().setId(21L);
        segunda.getMision().setTitulo("Separar residuos");
        segunda.getMision().setCategoria(CategoriaMision.RECICLAJE);
        segunda.getMision().setSemillasRecompensa(15);
        segunda.setFechaAsignacion(segundaFechaAsignacion);
        segunda.setFechaLimite(segundaFechaLimite);
        segunda.setAcreditada(null);
        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoMisionRepository.findByNinoTutorIdAndEstadoOrderByFechaAsignacionDesc(
                1L,
                EstadoMisionAsignada.EN_PROGRESO
        )).thenReturn(List.of(primera, segunda));

        List<MisionAsignadaResponse> response = ninoMisionService.listarPendientesPorTutor(" TUTOR@EMAIL.COM ");

        assertAll(
                () -> assertEquals(2, response.size()),
                () -> assertEquals(100L, response.get(0).id()),
                () -> assertEquals(10L, response.get(0).ninoId()),
                () -> assertEquals(20L, response.get(0).misionId()),
                () -> assertEquals("Cerrar la llave", response.get(0).tituloMision()),
                () -> assertEquals(CategoriaMision.AGUA, response.get(0).categoria()),
                () -> assertEquals(10, response.get(0).semillasRecompensa()),
                () -> assertEquals(EstadoMisionAsignada.EN_PROGRESO, response.get(0).estado()),
                () -> assertEquals(primeraFechaAsignacion, response.get(0).fechaAsignacion()),
                () -> assertNull(response.get(0).fechaCompletado()),
                () -> assertEquals(primeraFechaLimite, response.get(0).fechaLimite()),
                () -> assertEquals("http://example.com/primera.jpg", response.get(0).evidencia()),
                () -> assertFalse(response.get(0).acreditada()),
                () -> assertEquals(101L, response.get(1).id()),
                () -> assertEquals(11L, response.get(1).ninoId()),
                () -> assertEquals(21L, response.get(1).misionId()),
                () -> assertEquals("Separar residuos", response.get(1).tituloMision()),
                () -> assertEquals(CategoriaMision.RECICLAJE, response.get(1).categoria()),
                () -> assertEquals(15, response.get(1).semillasRecompensa()),
                () -> assertEquals(EstadoMisionAsignada.EN_PROGRESO, response.get(1).estado()),
                () -> assertEquals(segundaFechaAsignacion, response.get(1).fechaAsignacion()),
                () -> assertNull(response.get(1).fechaCompletado()),
                () -> assertEquals(segundaFechaLimite, response.get(1).fechaLimite()),
                () -> assertNull(response.get(1).evidencia()),
                () -> assertNull(response.get(1).acreditada())
        );
    }

    @Test
    void listarPendientesPorTutor_CuandoUsuarioAutenticadoNoExiste_DeberiaLanzarUsernameNotFoundException() {
        when(usuarioRepository.findByEmail("ausente@email.com")).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> ninoMisionService.listarPendientesPorTutor(" AUSENTE@EMAIL.COM ")
        );

        verify(ninoMisionRepository, never()).findByNinoTutorIdAndEstadoOrderByFechaAsignacionDesc(
                any(Long.class),
                any(EstadoMisionAsignada.class)
        );
    }
    }

    private void configurarConfirmacion(Usuario tutor, NinoMision asignacion) {
        configurarAsignacionParaConfirmar(tutor, asignacion);
        when(ninoMisionRepository.save(asignacion)).thenReturn(asignacion);
    }

    private void configurarAsignacionValida(Usuario tutor, Nino nino, Mision mision) {
        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoRepository.findById(10L)).thenReturn(Optional.of(nino));
        when(misionRepository.findById(20L)).thenReturn(Optional.of(mision));
        when(ninoMisionRepository.existsByNinoIdAndMisionIdAndEstado(
                10L,
                20L,
                EstadoMisionAsignada.EN_PROGRESO
        )).thenReturn(false);
    }

    private void configurarAsignacionParaConfirmar(Usuario tutor, NinoMision asignacion) {
        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoMisionRepository.findByIdForUpdate(asignacion.getId())).thenReturn(Optional.of(asignacion));
    }

    private void assertAsignacionSinMutacion(NinoMision asignacion) {
        assertAll(
                () -> assertEquals(EstadoMisionAsignada.EN_PROGRESO, asignacion.getEstado()),
                () -> assertNull(asignacion.getFechaCompletado()),
                () -> assertNull(asignacion.getEvidencia()),
                () -> assertFalse(asignacion.getAcreditada())
        );
    }

    private NinoMision crearAsignacion(Long id, Nino nino, EstadoMisionAsignada estado) {
        return NinoMision.builder()
                .id(id)
                .nino(nino)
                .mision(crearMision(20L, true))
                .estado(estado)
                .fechaAsignacion(LocalDateTime.now())
                .acreditada(false)
                .build();
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
