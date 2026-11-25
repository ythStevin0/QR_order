package com.qrorder.shared.config;

import com.qrorder.catalog.api.dto.MenuItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RedisCacheSerializationTest {

    @Test
    @DisplayName("activeMenuSerializer round-trips immutable List<MenuItemResponse> records cleanly")
    void activeMenuSerializerRoundTrip() {
        var serializer = RedisConfig.activeMenuSerializer(RedisConfig.createCacheObjectMapper());

        // Stream.toList() produces ImmutableCollections$ListN of final Java records
        List<MenuItemResponse> source = List.of(
                new MenuItemResponse(1L, "Mie Hompimpa", "Mie Pedas", "Pedas gurih", 15000L, null, true),
                new MenuItemResponse(2L, "Es Teh Manis", "Minuman Segar", "Segar", 5000L, null, true)
        );

        byte[] serialized = serializer.serialize(source);
        assertThat(serialized).isNotEmpty();

        List<MenuItemResponse> deserialized = serializer.deserialize(serialized);
        assertThat(deserialized).hasSize(2);
        assertThat(deserialized).containsExactlyElementsOf(source);
    }
}
