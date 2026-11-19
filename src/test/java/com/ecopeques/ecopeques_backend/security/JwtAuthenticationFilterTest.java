package com.ecopeques.ecopeques_backend.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtUtils, customUserDetailsService);
        request = new MockHttpServletRequest();
        request.setServletPath("/api/misiones");
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void filtrar_CuandoNoHayHeader_ContinuaSinAutenticar() throws Exception {
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtils, customUserDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void filtrar_CuandoHeaderNoEsBearer_ContinuaSinAutenticar() throws Exception {
        request.addHeader("Authorization", "Basic credenciales");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtils, customUserDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void filtrar_CuandoTokenEsInvalido_ContinuaSinConsultarUsuario() throws Exception {
        request.addHeader("Authorization", "Bearer token-invalido");
        when(jwtUtils.validateToken("token-invalido")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(jwtUtils).validateToken("token-invalido");
        verify(jwtUtils, never()).getEmailFromToken("token-invalido");
        verifyNoInteractions(customUserDetailsService);
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void filtrar_CuandoTokenEsValido_EstableceAutenticacionYContinua() throws Exception {
        request.addHeader("Authorization", "Bearer token-valido");
        UserDetails userDetails = User.builder()
                .username("padre@ecopeques.com")
                .password("password")
                .authorities("ROLE_PADRE")
                .build();
        when(jwtUtils.validateToken("token-valido")).thenReturn(true);
        when(jwtUtils.getEmailFromToken("token-valido")).thenReturn("padre@ecopeques.com");
        when(customUserDetailsService.loadUserByUsername("padre@ecopeques.com")).thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(authentication.isAuthenticated());
        assertSame(userDetails, authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PADRE")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void filtrar_CuandoYaExisteAutenticacion_NoLaReemplaza() throws Exception {
        Authentication existente = new UsernamePasswordAuthenticationToken(
                "usuario-existente",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_DOCENTE"))
        );
        SecurityContextHolder.getContext().setAuthentication(existente);
        request.addHeader("Authorization", "Bearer token-valido");
        when(jwtUtils.validateToken("token-valido")).thenReturn(true);
        when(jwtUtils.getEmailFromToken("token-valido")).thenReturn("otro@ecopeques.com");

        filter.doFilterInternal(request, response, filterChain);

        assertSame(existente, SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(customUserDetailsService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotFilter_CuandoRutaEsDeAutenticacion_RetornaTrue() {
        request.setServletPath("/api/auth/login");

        assertTrue(filter.shouldNotFilter(request));

        request.setServletPath("/api/usuarios");
        assertFalse(filter.shouldNotFilter(request));
    }
}
