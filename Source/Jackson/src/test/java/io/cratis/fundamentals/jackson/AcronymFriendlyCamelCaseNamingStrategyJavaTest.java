// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

final class AcronymFriendlyCamelCaseNamingStrategyJavaTest {
    public static final class Payload {
        public boolean IsCIA = true;
        public String FirstName = "x";
    }

    @Test
    void strategyCanBeConstructedAndUsedFromJava() {
        var strategy = new AcronymFriendlyCamelCaseNamingStrategy();
        assertEquals("isCIA", strategy.nameForField(null, null, "IsCIA"));
        assertNull(strategy.nameForField(null, null, null));

        var mapper = JsonMapper.builder().propertyNamingStrategy(strategy).build();
        assertEquals(mapper.readTree("{\"isCIA\":true,\"firstName\":\"x\"}"), mapper.readTree(mapper.writeValueAsString(new Payload())));
    }
}
