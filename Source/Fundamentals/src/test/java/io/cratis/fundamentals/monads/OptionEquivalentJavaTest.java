// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.monads;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Evidence for Monads/Option.cs and for_Option specs at 6ae7eb7 using JVM-native Optional. */
final class OptionEquivalentJavaTest {
    @Test
    void valueMapsHasValueTryGetValueMatchAndSwitch() {
        var value = Optional.of(42);
        assertTrue(value.isPresent());
        assertEquals(42, value.orElseThrow());
        assertEquals(42, value.map(number -> number).orElseGet(() -> -1));
        var calls = new ArrayList<Integer>();
        value.ifPresentOrElse(calls::add, () -> calls.add(-1));
        assertEquals(List.of(42), calls);
    }

    @Test
    void emptyMapsNoneAndSelectsOnlyTheAbsentBranch() {
        var none = Optional.<Integer>empty();
        assertFalse(none.isPresent());
        assertTrue(none.isEmpty());
        assertEquals(-1, none.map(number -> number).orElseGet(() -> -1));
        var calls = new ArrayList<Integer>();
        none.ifPresentOrElse(calls::add, () -> calls.add(-1));
        assertEquals(List.of(-1), calls);
    }
}
