package com.subzero.test

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Test

fun assertTrue(actual: Boolean, message: String? = null) {
    if (!actual) {
        throw AssertionError(message ?: "Expected true, but was false")
    }
}

fun assertFalse(actual: Boolean, message: String? = null) {
    if (actual) {
        throw AssertionError(message ?: "Expected false, but was true")
    }
}

fun assertEquals(expected: Any?, actual: Any?, message: String? = null) {
    if (expected != actual) {
        throw AssertionError(message ?: "Expected <$expected>, but was <$actual>")
    }
}

fun assertEquals(expected: Double, actual: Double, absoluteTolerance: Double, message: String? = null) {
    if (kotlin.math.abs(expected - actual) > absoluteTolerance) {
        throw AssertionError(message ?: "Expected <$expected> ± $absoluteTolerance, but was <$actual>")
    }
}

fun assertNotNull(actual: Any?, message: String? = null) {
    if (actual == null) {
        throw AssertionError(message ?: "Expected non-null value, but was null")
    }
}

fun assertNull(actual: Any?, message: String? = null) {
    if (actual != null) {
        throw AssertionError(message ?: "Expected null, but was <$actual>")
    }
}
