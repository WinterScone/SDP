package org.example.sdpclient.configuration;


import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAuthenticationInterceptorTest {

    private final AdminAuthenticationInterceptor interceptor = new AdminAuthenticationInterceptor();

    @Test
    void preHandle_blocks_whenCookiesNull() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override
            public Cookie[] getCookies() {
                return null;
            }
        };
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString())
                .isEqualTo("{\"ok\":false,\"message\":\"Unauthorized\"}");
    }

    @Test
    void preHandle_blocks_whenCookieMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("someOtherCookie", "x"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString())
                .isEqualTo("{\"ok\":false,\"message\":\"Unauthorized\"}");
    }

    @Test
    void preHandle_blocks_whenCookieBlank() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("adminUsername", ""));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString())
                .isEqualTo("{\"ok\":false,\"message\":\"Unauthorized\"}");
    }

    @Test
    void preHandle_blocks_whenCookieWhitespace() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("adminUsername", "   "));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString())
                .isEqualTo("{\"ok\":false,\"message\":\"Unauthorized\"}");
    }

    @Test
    void preHandle_allows_whenCookiePresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("adminUsername", "alice"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        // response should remain untouched
        assertThat(response.getStatus()).isEqualTo(200); // default for MockHttpServletResponse
        assertThat(response.getContentType()).isNull();
        assertThat(response.getContentAsString()).isEmpty();
    }
}

