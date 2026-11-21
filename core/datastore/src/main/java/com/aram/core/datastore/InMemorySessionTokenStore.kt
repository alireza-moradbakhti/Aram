package com.aram.core.datastore

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/** Alpha-flavor store: a session ends when the process does. */
class InMemorySessionTokenStore @Inject constructor() : SessionTokenStore {

    private val token = AtomicReference<SessionToken?>(null)

    override suspend fun read(): SessionToken? = token.get()

    override suspend fun write(token: SessionToken) {
        this.token.set(token)
    }

    override suspend fun clear() {
        token.set(null)
    }
}
