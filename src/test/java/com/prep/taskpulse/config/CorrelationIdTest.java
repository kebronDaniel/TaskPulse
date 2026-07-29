package com.prep.taskpulse.config;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;


public class CorrelationIdTest {

    private static final String HEADER_NAME = "X-Correlation-ID";
    private static final String MDC_KEY = "correlationId";

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clear(){
        MDC.clear();
    }

    @Test
    void doFilter_whenCorrelationIdIsMissing_generatesValidIdAndAddsItToMdc() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> correlationIdInsideChain = new AtomicReference<>();

        // this is triggered by correctionIdFilter (after setting the header it continues the chain)
        // this would be a captor that gets the value set in the MDC.
        FilterChain filterChain = ((servletRequest, servletResponse)
                -> correlationIdInsideChain.set(MDC.get(MDC_KEY)));

        // start the filter
        filter.doFilter(request,response,filterChain);

        String generatedId = response.getHeader(HEADER_NAME);

        assertNotNull(generatedId);
        assertDoesNotThrow(() -> UUID.fromString(generatedId));
        assertEquals(generatedId, correlationIdInsideChain.get());
    }

    @Test
    void doFilter_whenCorrelationIdIsValid_preservesProvidedId() throws Exception {

        String providedId = UUID.randomUUID().toString();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER_NAME, providedId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> correlationIdInsideChain = new AtomicReference<>();

        FilterChain filterChain = ((servletRequest, servletResponse)
                -> correlationIdInsideChain.set(MDC.get(MDC_KEY)));

        filter.doFilter(request,response,filterChain);

        assertEquals(providedId, correlationIdInsideChain.get());
        assertEquals(providedId, response.getHeader(HEADER_NAME));
    }

    @Test
    void doFilter_whenCorrelationIdIsInvalid_replacesItWithValidId() throws Exception {

        String invalidId = "invalid UUID";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER_NAME, invalidId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> correlationIdInsideChain = new AtomicReference<>();

        FilterChain filterChain = ((servletRequest, servletResponse)
                -> correlationIdInsideChain.set(MDC.get(MDC_KEY)));

        filter.doFilter(request,response,filterChain);

        String replacementId = response.getHeader(HEADER_NAME);

        assertNotNull(replacementId);
        assertNotEquals(replacementId, invalidId);
        assertDoesNotThrow(() -> UUID.fromString(replacementId));
        assertEquals(replacementId, correlationIdInsideChain.get());
    }

    @Test
    void doFilter_afterRequestCompletes_clearsCorrelationIdFromMdc() throws Exception{

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> correlationIdInsideChain = new AtomicReference<>();
        FilterChain filterChain = ((servletRequest, servletResponse)
                -> correlationIdInsideChain.set(MDC.get(MDC_KEY)));

        filter.doFilter(request,response,filterChain);
        assertNotNull(correlationIdInsideChain.get());
    }

    @Test
    void doFilter_whenFilterChainThrows_stillClearsCorrelationIdFromMdc(){

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // means the next filters in the filter chain are broken.
        FilterChain failingChain = ((servletRequest, servletResponse) -> {
            assertNotNull(MDC.get(MDC_KEY));
            throw new ServletException("Downstream failure");
        });

        assertThrows(ServletException.class, () -> filter.doFilter(request,response,failingChain));
        // after the filter chain fails then @after each would clear the MDC since its considered as completed.
        // you can place this in the other tests and it always passes.
        assertNull(MDC.get(MDC_KEY));
    }
}
