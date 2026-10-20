package com.qrorder.shared.pagination;

import java.util.List;

/**
 * Generic response container for Keyset / Cursor-based Pagination (Topik 18).
 * Guarantees O(log N) lookup regardless of data size.
 */
public record CursorPageResponse<T>(
        List<T> data,
        String nextCursor,
        boolean hasMore,
        int pageSize
) {
    public static <T> CursorPageResponse<T> of(List<T> data, String nextCursor, boolean hasMore, int pageSize) {
        return new CursorPageResponse<>(data, nextCursor, hasMore, pageSize);
    }
}
