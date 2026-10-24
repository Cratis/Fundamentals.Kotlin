// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.cratis.fundamentals.concepts.ConceptAs;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class WireContractsJavaConformanceTest {
    enum Status {
        @JsonProperty("active") ACTIVE,
        @JsonAlias("off") INACTIVE
    }

    record Window(Duration span) {}
    record OrderId(UUID value) implements ConceptAs<UUID> {}

    @Test
    void enumValuesUseOrdinalsAndReadNamesAliasesAndNumericStrings() {
        var mapper = JsonMapper.builder().addModule(new EnumModule()).build();
        assertEquals("0", mapper.writeValueAsString(Status.ACTIVE));
        assertEquals(Status.ACTIVE, mapper.readValue("\"active\"", Status.class));
        assertEquals(Status.INACTIVE, mapper.readValue("\"off\"", Status.class));
        assertEquals(Status.INACTIVE, mapper.readValue("\"1\"", Status.class));
    }

    @Test
    void durationsUseTimeSpanStringsInRecordProperties() {
        var mapper = JsonMapper.builder().addModule(new TimeSpanModule()).build();
        var value = new Window(Duration.ofSeconds(93784));
        var json = "{\"span\":\"1.02:03:04\"}";
        assertEquals(json, mapper.writeValueAsString(value));
        assertEquals(value, mapper.readValue(json, Window.class));
    }

    @Test
    void embeddedConceptKeyOptionWritesJsonAndReadsBothFormats() {
        var mapper = JsonMapper.builder()
            .addModule(new ConceptsModule(ConceptKeyFormat.EMBEDDED_JSON)).build();
        var id = new OrderId(UUID.fromString("6f9619ff-8b86-d011-b42d-00c04fc964ff"));
        var values = Map.of(id, 1);
        var embedded = "{\"\\\"6f9619ff-8b86-d011-b42d-00c04fc964ff\\\"\":1}";
        var plain = "{\"6f9619ff-8b86-d011-b42d-00c04fc964ff\":1}";
        var type = new TypeReference<Map<OrderId, Integer>>() {};
        assertEquals(embedded, mapper.writeValueAsString(values));
        assertEquals(values, mapper.readValue(embedded, type));
        assertEquals(values, mapper.readValue(plain, type));
        var defaults = JsonMapper.builder().addModule(new ConceptsModule()).build();
        var explicitPlain = JsonMapper.builder().addModule(new ConceptsModule(ConceptKeyFormat.PLAIN)).build();
        assertEquals(plain, defaults.writeValueAsString(values));
        assertEquals(plain, explicitPlain.writeValueAsString(values));
    }
}
