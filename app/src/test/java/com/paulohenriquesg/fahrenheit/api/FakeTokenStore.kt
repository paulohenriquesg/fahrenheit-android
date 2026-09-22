package com.paulohenriquesg.fahrenheit.api

import com.paulohenriquesg.fahrenheit.auth.StoredCredentials
import com.paulohenriquesg.fahrenheit.auth.TokenStore

/**
 * In-memory [TokenStore] so session tests need no Android framework.
 *
 * Whether real SharedPreferences round-trip correctly is a different question,
 * covered by SharedPreferencesTokenStoreTest against the real implementation.
 */
class FakeTokenStore(
    private var credentials: StoredCredentials = StoredCredentials.EMPTY
) : TokenStore {
    var writes: Int = 0
        private set

    override fun read(): StoredCredentials = credentials

    override fun write(credentials: StoredCredentials) {
        this.credentials = credentials
        writes++
    }
}
