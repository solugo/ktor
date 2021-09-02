/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.sessions

import io.ktor.utils.io.*

/**
 * Represents a way to [write], [read] and [invalidate] session bits.
 */
public interface SessionStorage {
    /**
     * Writes a session [id] using a specific [provider].
     *
     * This method calls the [provider] with a [ByteWriteChannel] and it is in charge of the channel's lifecycle.
     * [provider] is in charge of writing session bits to the specified [ByteWriteChannel].
     */
    public suspend fun write(id: String, provider: suspend (ByteWriteChannel) -> Unit)

    /**
     * Invalidates session [id].
     *
     * This method prevents session [id] from being accessible after this call.
     *
     * @throws NoSuchElementException when session [id] is not found.
     */
    public suspend fun invalidate(id: String)

    /**
     * Reads session [id] using a [consumer] as [R]
     *
     * This method calls the [consumer] with a [ByteReadChannel] and it is in charge of the channel's lifecycle,
     * and returns the object [R] produced by the [consumer].
     * [consumer] should read the content of the specified [ByteReadChannel] and return an object of type [R].
     *
     * @return instance of [R] representing the session object returned from the [consumer].
     * @throws NoSuchElementException when session [id] is not found.
     */
    public suspend fun <R> read(id: String, consumer: suspend (ByteReadChannel) -> R): R
}
