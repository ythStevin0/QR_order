package com.qrorder.shared.pagination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CursorUtilsTest {

    @Test
    @DisplayName("encode and decode valid cursor roundtrip")
    void testEncodeDecodeRoundtrip() {
        Instant now = Instant.now();
        UUID orderId = UUID.randomUUID();

        String cursor = CursorUtils.encode(now, orderId);
        assertThat(cursor).isNotNull().isNotBlank();

        CursorUtils.OrderCursor decoded = CursorUtils.decode(cursor);
        assertThat(decoded).isNotNull();
        assertThat(decoded.createdAt()).isEqualTo(now);
        assertThat(decoded.id()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("decode invalid or null cursor returns null safely without exception")
    void testDecodeInvalidCursor() {
        assertThat(CursorUtils.decode(null)).isNull();
        assertThat(CursorUtils.decode("")).isNull();
        assertThat(CursorUtils.decode("invalid_base64_%%%")).isNull();
        assertThat(CursorUtils.decode("bm9fc2VwYXJhdG9y")).isNull(); // base64 of "no_separator"
    }
}
