// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.monads;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CatchJavaTest {
    private enum TheErrorType { SOME_TYPE, SOME_OTHER_TYPE }
    private final IOException original = new IOException("original");

    @Test
    void noValueCasesExposeFactoriesAccessorsMatchAndSwitch() {
        var success = Catch.success();
        var caught = Catch.failed(original);
        assertTrue(success.isSuccess());
        assertTrue(success.tryGetException().isEmpty());
        assertEquals("none", success.match(() -> "none", Exception::getMessage));
        assertSame(Catch.Success.INSTANCE, success);
        assertFalse(caught.isSuccess());
        assertSame(original, caught.tryGetException().get());
        assertSame(original, caught.match(() -> null, error -> error));
        var calls = new ArrayList<String>();
        success.switchOn(() -> calls.add("none"), error -> calls.add(error.getMessage()));
        caught.switchOn(() -> calls.add("none"), error -> calls.add(error.getMessage()));
        assertEquals(List.of("none", "original"), calls);
    }

    @Test
    void valueCasesExposeTargetTypingAccessorsMatchAndSwitch() {
        CatchWithValue<Integer> success = CatchWithValue.success(42);
        CatchWithValue<Integer> caught = CatchWithValue.failed(original);
        assertTrue(success.isSuccess());
        assertEquals(Optional.of(42), success.tryGetResult());
        assertTrue(success.tryGetException().isEmpty());
        assertEquals(Integer.valueOf(42), success.match(value -> value, error -> -1));
        assertFalse(caught.isSuccess());
        assertTrue(caught.tryGetResult().isEmpty());
        assertSame(original, caught.tryGetException().get());
        assertSame(original, caught.match(value -> null, error -> error));
        var calls = new ArrayList<String>();
        success.switchOn(value -> calls.add(value.toString()), error -> calls.add(error.getMessage()));
        caught.switchOn(value -> calls.add(value.toString()), error -> calls.add(error.getMessage()));
        assertEquals(List.of("42", "original"), calls);
        if (success instanceof CatchWithValue.Success<Integer> s) {
            assertEquals(42, s.getValue());
        } else {
            throw new AssertionError("Expected Success");
        }
    }

    @Test
    void threeWayFactoriesResolveOverloadsAndExposeAllBranches() {
        CatchWithError<Integer, TheErrorType> success = CatchWithError.success(42);
        CatchWithError<Integer, TheErrorType> failure = CatchWithError.failed(TheErrorType.SOME_OTHER_TYPE);
        CatchWithError<Integer, TheErrorType> caught = CatchWithError.failed(original);
        assertTrue(success.isSuccess());
        assertEquals(Optional.of(42), success.tryGetResult());
        assertTrue(success.tryGetError().isEmpty());
        assertTrue(success.tryGetException().isEmpty());
        assertEquals(Integer.valueOf(42), success.match(value -> value, error -> -1, error -> -2));
        assertFalse(failure.isSuccess());
        assertTrue(failure.tryGetResult().isEmpty());
        assertEquals(TheErrorType.SOME_OTHER_TYPE, failure.tryGetError().get());
        assertTrue(failure.tryGetException().isEmpty());
        assertEquals(TheErrorType.SOME_OTHER_TYPE, failure.match(value -> null, error -> error, error -> null));
        assertFalse(caught.isSuccess());
        assertTrue(caught.tryGetResult().isEmpty());
        assertTrue(caught.tryGetError().isEmpty());
        assertSame(original, caught.tryGetException().get());
        assertSame(original, caught.match(value -> null, error -> null, error -> error));
        var calls = new ArrayList<String>();
        for (var result : List.of(success, failure, caught)) {
            result.switchOn(value -> calls.add(value.toString()), error -> calls.add(error.name()),
                error -> calls.add(error.getMessage()));
        }
        assertEquals(List.of("42", "SOME_OTHER_TYPE", "original"), calls);
        assertTrue(failure instanceof CatchWithError.Failure<Integer, TheErrorType>);
        if (caught instanceof CatchWithError.Caught<Integer, TheErrorType> c) {
            assertSame(original, c.getException());
        } else {
            throw new AssertionError("Expected Caught");
        }
    }

    @Test
    void rethrowPreservesCheckedExceptionIdentityAndStackForAllVariants() {
        var stack = original.getStackTrace().clone();
        CatchWithValue<Integer> value = CatchWithValue.failed(original);
        CatchWithError<Integer, TheErrorType> typed = CatchWithError.failed(original);
        List<Executable> rethrows = List.of(Catch.failed(original)::rethrowError, value::rethrowError, typed::rethrowError);
        for (var rethrow : rethrows) {
            assertSame(original, assertThrows(IOException.class, rethrow));
            assertArrayEquals(stack, original.getStackTrace());
        }
    }

    @Test
    void checkedIOExceptionCanBeCaughtByName() throws Exception {
        try {
            Catch.failed(original).rethrowError();
            throw new AssertionError("Expected IOException");
        } catch (IOException exception) {
            assertSame(original, exception);
        }
    }

    @Test
    void successAndTypedErrorDoNotThrow() {
        assertDoesNotThrow(() -> {
            Catch.success().rethrowError();
            CatchWithValue.success(42).rethrowError();
            CatchWithError.<Integer, TheErrorType>success(42).rethrowError();
            CatchWithError.<Integer, TheErrorType>failed(TheErrorType.SOME_TYPE).rethrowError();
            new CatchWithError.Failure<Integer, IOException>(original).rethrowError();
        });
    }

    @Test
    void nullPayloadsAreRejected() {
        assertThrows(NullPointerException.class, () -> Catch.failed(null));
        assertThrows(NullPointerException.class, () -> CatchWithValue.failed(null));
        assertThrows(NullPointerException.class, () -> CatchWithValue.success(null));
        assertThrows(NullPointerException.class, () -> CatchWithError.failed((Exception) null));
        assertThrows(NullPointerException.class, () -> CatchWithError.failed((TheErrorType) null));
        assertThrows(NullPointerException.class, () -> CatchWithError.success(null));
    }
}
