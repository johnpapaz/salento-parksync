package com.parksync.unit.auth;

import com.parksync.auth.JwtAuthenticationFilter;
import com.parksync.auth.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        filter = new JwtAuthenticationFilter(jwtService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testFilterNoAuthorizationHeader() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testFilterHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Basic cGFzc3dvcmQ=");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testFilterTokenInvalid() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer invalidtoken");
        when(jwtService.isValid("invalidtoken")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testFilterTokenValidButNoUsername() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        Claims claims = mock(Claims.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer validtoken");
        when(jwtService.isValid("validtoken")).thenReturn(true);
        when(jwtService.validateAndExtract("validtoken")).thenReturn(claims);
        when(claims.getSubject()).thenReturn(null);
        when(claims.get("role", String.class)).thenReturn("ADMIN");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testFilterTokenValidButNoRole() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        Claims claims = mock(Claims.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer validtoken");
        when(jwtService.isValid("validtoken")).thenReturn(true);
        when(jwtService.validateAndExtract("validtoken")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("user");
        when(claims.get("role", String.class)).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testFilterTokenValidWithRoleWithoutRolePrefix() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        Claims claims = mock(Claims.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer validtoken");
        when(jwtService.isValid("validtoken")).thenReturn(true);
        when(jwtService.validateAndExtract("validtoken")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("user");
        when(claims.get("role", String.class)).thenReturn("ADMIN");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("user", auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void testFilterTokenValidWithRoleWithRolePrefix() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        Claims claims = mock(Claims.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer validtoken");
        when(jwtService.isValid("validtoken")).thenReturn(true);
        when(jwtService.validateAndExtract("validtoken")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("user");
        when(claims.get("role", String.class)).thenReturn("ROLE_OPERATOR");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("user", auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_OPERATOR")));
    }
}
