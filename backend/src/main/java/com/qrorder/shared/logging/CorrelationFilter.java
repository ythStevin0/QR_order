package com.qrorder.shared.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Injects a unique traceId into SLF4J MDC for correlation logging across the entire request lifecycle.
 * Highest precedence filter to ensure all subsequent log lines carry the traceId.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String OUTLET_ID_MDC_KEY = "outletId";
    public static final String ORDER_ID_MDC_KEY = "orderId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String OUTLET_ID_HEADER = "X-Outlet-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = Optional.ofNullable(request.getHeader(REQUEST_ID_HEADER))
                .filter(id -> !id.isBlank() && id.length() <= 64 && id.matches("^[a-zA-Z0-9_-]+$"))
                .orElseGet(() -> UUID.randomUUID().toString());

        MDC.put(TRACE_ID_MDC_KEY, traceId);
        response.setHeader(REQUEST_ID_HEADER, traceId);

        // Check if outletId is provided in header or param
        String outletId = request.getHeader(OUTLET_ID_HEADER);
        if (outletId == null || outletId.isBlank()) {
            outletId = request.getParameter("outletId");
        }
        if (outletId != null && !outletId.isBlank()) {
            MDC.put(OUTLET_ID_MDC_KEY, outletId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    public static void setOutletId(Long outletId) {
        if (outletId != null) {
            MDC.put(OUTLET_ID_MDC_KEY, String.valueOf(outletId));
        }
    }

    public static void setOrderId(UUID orderId) {
        if (orderId != null) {
            MDC.put(ORDER_ID_MDC_KEY, orderId.toString());
        }
    }
}
