package com.example.eventmanagement.data.firebase

import com.example.eventmanagement.core.error.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseErrorMapperTest {

    @Test
    fun mapsNetworkHintsFromMessage() {
        val mapped = mapFirebaseException(RuntimeException("Network request failed"))
        assertEquals(AppError.Network, mapped)
    }

    @Test
    fun mapsUnknownFallback() {
        val mapped = mapFirebaseException(IllegalStateException("boom"))
        assertTrue(mapped is AppError.Unknown)
    }
}
