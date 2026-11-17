package com.ecopeques.ecopeques_backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecopeques.ecopeques_backend.model.Rol;
import com.ecopeques.ecopeques_backend.model.Usuario;
import com.ecopeques.ecopeques_backend.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new CustomUserDetailsService(usuarioRepository);
    }

    @ParameterizedTest
    @EnumSource(Rol.class)
    void cargarUsuario_CuandoExiste_AsignaAutoridadSegunRol(Rol rol) {
        Usuario usuario = Usuario.builder()
                .email("usuario@ecopeques.com")
                .password("password-encriptado")
                .rol(rol)
                .build();
        when(usuarioRepository.findByEmail("usuario@ecopeques.com")).thenReturn(Optional.of(usuario));

        UserDetails resultado = userDetailsService.loadUserByUsername(" USUARIO@ECOPEQUES.COM ");

        assertEquals("usuario@ecopeques.com", resultado.getUsername());
        assertEquals("password-encriptado", resultado.getPassword());
        assertEquals(1, resultado.getAuthorities().size());
        assertEquals("ROLE_" + rol.name(), resultado.getAuthorities().iterator().next().getAuthority());
        verify(usuarioRepository).findByEmail("usuario@ecopeques.com");
    }

    @Test
    void cargarUsuario_CuandoNoExiste_LanzaUsernameNotFoundException() {
        when(usuarioRepository.findByEmail("inexistente@ecopeques.com")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("inexistente@ecopeques.com")
        );

        assertEquals("Usuario no encontrado", exception.getMessage());
    }
}
