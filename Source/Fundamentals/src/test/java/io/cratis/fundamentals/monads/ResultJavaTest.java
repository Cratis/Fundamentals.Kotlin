// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.monads;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ResultJavaTest {
    private enum TheErrorType { SOME_TYPE, SOME_OTHER_TYPE }

    @Test
    void plainFactoriesExposeSingletons() {
        assertTrue(Result.success().isSuccess());
        assertFalse(Result.failed().isSuccess());
        assertSame(Result.Success.INSTANCE, Result.success());
        assertSame(Result.Failure.INSTANCE, Result.failed());
    }

    @Test
    void typedErrorsSupportAccessorsMatchSwitchAndPatterns() {
        ResultWithError<TheErrorType> success = ResultWithError.success();
        ResultWithError<TheErrorType> failure = ResultWithError.failed(TheErrorType.SOME_TYPE);
        assertTrue(success.isSuccess());
        assertFalse(success.tryGetError().isPresent());
        assertEquals("none", success.match(() -> "none", Enum::name));
        assertFalse(failure.isSuccess());
        assertTrue(failure.tryGetError().isPresent());
        assertEquals(TheErrorType.SOME_TYPE, failure.tryGetError().get());
        assertEquals("SOME_TYPE", failure.match(() -> "none", Enum::name));
        var calls = new ArrayList<String>();
        success.switchOn(() -> calls.add("none"), error -> calls.add(error.name()));
        failure.switchOn(() -> calls.add("none"), error -> calls.add(error.name()));
        assertEquals(List.of("none", "SOME_TYPE"), calls);
        assertTrue(success instanceof ResultWithError.Success<TheErrorType>);
        if (failure instanceof ResultWithError.Failure<TheErrorType> f) {
            assertEquals(TheErrorType.SOME_TYPE, f.getError());
        } else {
            throw new AssertionError("Expected Failure");
        }
    }

    @Test
    void valuesSupportTargetTypingAccessorsMatchSwitchAndPatterns() {
        ResultWithValue<Integer, TheErrorType> success = ResultWithValue.success(42);
        ResultWithValue<Integer, TheErrorType> failure = ResultWithValue.failed(TheErrorType.SOME_OTHER_TYPE);
        assertTrue(success.isSuccess());
        assertEquals(Optional.of(42), success.tryGetResult());
        assertFalse(success.tryGetError().isPresent());
        assertEquals(Integer.valueOf(42), success.match(value -> value, error -> -1));
        assertFalse(failure.isSuccess());
        assertFalse(failure.tryGetResult().isPresent());
        assertEquals(TheErrorType.SOME_OTHER_TYPE, failure.tryGetError().get());
        assertEquals("SOME_OTHER_TYPE", failure.match(Object::toString, Enum::name));
        var calls = new ArrayList<String>();
        success.switchOn(value -> calls.add(value.toString()), error -> calls.add(error.name()));
        failure.switchOn(value -> calls.add(value.toString()), error -> calls.add(error.name()));
        assertEquals(List.of("42", "SOME_OTHER_TYPE"), calls);
        if (success instanceof ResultWithValue.Success<Integer, TheErrorType> s) {
            assertEquals(42, s.getValue());
        } else {
            throw new AssertionError("Expected Success");
        }
        if (failure instanceof ResultWithValue.Failure<Integer, TheErrorType> f) {
            assertEquals(TheErrorType.SOME_OTHER_TYPE, f.getError());
        } else {
            throw new AssertionError("Expected Failure");
        }
    }

    @Test
    void nullPayloadsAreRejected() {
        assertThrows(NullPointerException.class, () -> ResultWithValue.success(null));
        assertThrows(NullPointerException.class, () -> ResultWithValue.failed(null));
        assertThrows(NullPointerException.class, () -> ResultWithError.failed(null));
    }
}
