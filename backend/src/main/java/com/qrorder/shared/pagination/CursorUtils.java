package com.qrorder.shared.pagination;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Encodes and decodes composite cursor (createdAt, id) for Keyset Pagination (Topik 18).
 */
public final class CursorUtils {

    private CursorUtils() {}

    public record OrderCursor(Instant createdAt, UUID id) {}

    public static String encode(Instant createdAt, UUID id) {
        if (createdAt == null || id == null) {
            return null;
        }
        String raw = createdAt.toString() + "#" + id.toString();
        return Base64.getUrlEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static OrderCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(cursor);
            String raw = new String(decodedBytes, StandardCharsets.UTF_8);
            String[] parts = raw.split("#");
            if (parts.length != 2) {
                return null;
            }
            Instant createdAt = Instant.parse(parts[0]);
            UUID id = UUID.fromString(parts[1]);
            return new OrderCursor(createdAt, id);
        } catch (Exception e) {
            return null;
        }
    }
}
