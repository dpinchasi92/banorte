package com.cbarkinozer.onlinebankingrestapi.app.sec.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtAuthenticationEntryPointTest {

    @Test
    void shouldReturnGenericMessageWithoutExceptionDetails() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new JwtAuthenticationEntryPoint().commence(new MockHttpServletRequest(), response,
                new BadCredentialsException("internal detail: user 42 token expired at 2026-01-01"));

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertEquals(JwtAuthenticationEntryPoint.UNAUTHORIZED_MESSAGE, response.getErrorMessage());
    }
}
