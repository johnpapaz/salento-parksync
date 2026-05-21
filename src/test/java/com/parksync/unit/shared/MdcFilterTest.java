package com.parksync.unit.shared;

import com.parksync.shared.MdcFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MdcFilterTest {

    private final MdcFilter mdcFilter = new MdcFilter();

    @Test
    void testFilterWithNullCorrelationIdHeader() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("X-Correlation-ID")).thenReturn(null);

        AtomicReference<String> capturedId = new AtomicReference<>();
        doAnswer(invocation -> {
            capturedId.set(MDC.get("correlationId"));
            return null;
        }).when(filterChain).doFilter(request, response);

        mdcFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(capturedId.get());
        assertDoesNotThrow(() -> UUID.fromString(capturedId.get()));
        assertNull(MDC.get("correlationId"), "MDC should be cleaned up after request");
    }

    @Test
    void testFilterWithEmptyCorrelationIdHeader() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("X-Correlation-ID")).thenReturn("   ");

        AtomicReference<String> capturedId = new AtomicReference<>();
        doAnswer(invocation -> {
            capturedId.set(MDC.get("correlationId"));
            return null;
        }).when(filterChain).doFilter(request, response);

        mdcFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(capturedId.get());
        assertDoesNotThrow(() -> UUID.fromString(capturedId.get()));
        assertNull(MDC.get("correlationId"), "MDC should be cleaned up after request");
    }

    @Test
    void testFilterWithValidCorrelationIdHeader() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        String customId = "my-custom-correlation-id-123";
        when(request.getHeader("X-Correlation-ID")).thenReturn(customId);

        AtomicReference<String> capturedId = new AtomicReference<>();
        doAnswer(invocation -> {
            capturedId.set(MDC.get("correlationId"));
            return null;
        }).when(filterChain).doFilter(request, response);

        mdcFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(customId, capturedId.get());
        assertNull(MDC.get("correlationId"), "MDC should be cleaned up after request");
    }
}
