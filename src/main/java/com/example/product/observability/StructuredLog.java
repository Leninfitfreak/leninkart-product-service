package com.example.product.observability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StructuredLog {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SERVICE_NAME = "product-service";

    private StructuredLog() {
    }

    public static void info(Logger logger, String operation, String message, Map<String, Object> extraFields) {
        logger.info(toJson("INFO", operation, message, null, extraFields));
    }

    public static void error(Logger logger, String operation, String message, Throwable error, Map<String, Object> extraFields) {
        logger.error(toJson("ERROR", operation, message, error, extraFields));
    }

    private static String toJson(String level, String operation, String message, Throwable error, Map<String, Object> extraFields) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", Instant.now().toString());
        payload.put("level", level);
        payload.put("service", SERVICE_NAME);
        payload.put("operation", operation);
        payload.put("message", message);

        SpanContext spanContext = Span.current().getSpanContext();
        if (spanContext.isValid()) {
            payload.put("trace_id", spanContext.getTraceId());
            payload.put("span_id", spanContext.getSpanId());
        } else {
            payload.put("trace_id", "");
            payload.put("span_id", "");
        }

        if (extraFields != null) {
            payload.putAll(extraFields);
        }

        if (error != null) {
            payload.put("error_type", error.getClass().getName());
            payload.put("error_message", error.getMessage());
            payload.put("stack_trace", stackTrace(error));
        }

        try {
            return MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{\"level\":\"ERROR\",\"service\":\"" + SERVICE_NAME + "\",\"operation\":\"" + operation + "\",\"message\":\"failed to serialize structured log\",\"error_message\":\"" + escape(e.getMessage()) + "\"}";
        }
    }

    private static String stackTrace(Throwable error) {
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }
}
