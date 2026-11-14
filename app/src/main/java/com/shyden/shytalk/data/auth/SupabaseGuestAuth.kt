package com.shyden.shytalk.data.auth

import com.shyden.shytalk.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns the app's invisible Supabase identity.
 *
 * A user never sees a login form. The first launch creates a Supabase
 * anonymous user; later launches restore the persisted session automatically.
 * The identity is still a real Supabase Auth user and therefore receives an
 * authenticated JWT for RLS.
 */
class SupabaseGuestAuth {
    private val mutex = Mutex()

    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
    ) {
        install(Auth)
    }

    suspend fun ensureSession(): String =
        mutex.withLock {
            client.auth.currentUserOrNull()?.id
                ?: client.auth.signInAnonymously().id
        }

    fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun signOut() {
        client.auth.signOut()
    }
}
