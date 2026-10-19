package com.qrorder.shared.error;

import com.qrorder.shared.logging.CorrelationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler emitting RFC 7807 Problem Details.
 * Enforces:
 * - 4xx errors logged at WARN/INFO without stack trace.
 * - 5xx errors logged at ERROR with stack trace.
 * - All responses include 'traceId' and 'timestamp' for correlation.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String getTraceId() {
        String traceId = MDC.get(CorrelationFilter.TRACE_ID_MDC_KEY);
        return traceId != null ? traceId : "unknown";
    }

    private ProblemDetail buildProblem(HttpStatus status, String title, String detail, String typeSlug) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://qrorder.com/errors/" + typeSlug));
        problem.setProperty("traceId", getTraceId());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found [traceId={}]: {}", getTraceId(), ex.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage(), "not-found");
    }

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequest(BadRequestException ex) {
        log.warn("Bad request [traceId={}]: {}", getTraceId(), ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), "bad-request");
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ProblemDetail handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized access [traceId={}]: {}", getTraceId(), ex.getMessage());
        return buildProblem(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), "unauthorized");
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request [traceId={}]: {}", getTraceId(), ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, "Malformed JSON Request", "The request body could not be parsed or contains unrecognized fields", "malformed-json");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        log.warn("Validation failure [traceId={}]: {}", getTraceId(), fieldErrors);
        ProblemDetail problem = buildProblem(HttpStatus.BAD_REQUEST, "Validation Error", "Invalid request body parameters", "validation-error");
        problem.setProperty("invalidFields", fieldErrors);
        return problem;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.warn("Concurrent update collision [traceId={}]: {}", getTraceId(), ex.getMessage());
        return buildProblem(HttpStatus.CONFLICT, "Concurrent Modification Conflict",
                "Data has been modified by another request. Please refresh and try again.", "concurrent-conflict");
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ProblemDetail handleIdempotencyConflict(IdempotencyConflictException ex) {
        log.warn("Idempotency conflict [traceId={}]: {}", getTraceId(), ex.getMessage());
        return buildProblem(HttpStatus.UNPROCESSABLE_ENTITY, "Idempotency Conflict", ex.getMessage(), "idempotency-conflict");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state operation [traceId={}]: {}", getTraceId(), ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, "Invalid Operation", ex.getMessage(), "invalid-state");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled internal server error [traceId={}]", getTraceId(), ex);
        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected internal error occurred. Please contact support quoting traceId: " + getTraceId(), "internal-error");
    }
}
