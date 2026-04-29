package com.actacofrade.backend.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private UserDetails ud(String email) {
        return new User(email, "pw", Collections.emptyList());
    }

    @Test
    void noHeader_chainContinuesUnauthenticated() throws Exception {
        HttpServletRequest req = new MockHttpServletRequest();
        HttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, filterChain);

        verify(filterChain, times(1)).doFilter(req, res);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void invalidScheme_skips() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Basic abc");
        filter.doFilter(req, new MockHttpServletResponse(), filterChain);
        verify(jwtService, never()).extractEmail(any());
    }

    @Test
    void validToken_setsAuthentication() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer tok");
        UserDetails u = ud("u@e.com");
        when(jwtService.extractEmail("tok")).thenReturn("u@e.com");
        when(userDetailsService.loadUserByUsername("u@e.com")).thenReturn(u);
        when(jwtService.isTokenValid("tok", u)).thenReturn(true);

        filter.doFilter(req, new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(u);
    }

    @Test
    void invalidToken_doesNotAuthenticate() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer tok");
        UserDetails u = ud("u@e.com");
        when(jwtService.extractEmail("tok")).thenReturn("u@e.com");
        when(userDetailsService.loadUserByUsername("u@e.com")).thenReturn(u);
        when(jwtService.isTokenValid("tok", u)).thenReturn(false);

        filter.doFilter(req, new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void jwtException_swallowed() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer bad");
        when(jwtService.extractEmail("bad")).thenThrow(new JwtException("bad"));
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, filterChain);

        verify(filterChain).doFilter(req, res);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void usernameNotFound_swallowed() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer tok");
        when(jwtService.extractEmail("tok")).thenReturn("u@e.com");
        when(userDetailsService.loadUserByUsername("u@e.com"))
                .thenThrow(new UsernameNotFoundException("no"));

        filter.doFilter(req, new MockHttpServletResponse(), filterChain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void unexpectedException_swallowed() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer tok");
        when(jwtService.extractEmail("tok")).thenThrow(new RuntimeException("boom"));

        filter.doFilter(req, new MockHttpServletResponse(), filterChain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
