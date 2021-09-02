/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.cio

import io.ktor.http.cio.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import java.net.*
import kotlin.coroutines.*

@OptIn(EngineAPI::class)
internal class CIOApplicationCall(
    application: Application,
    _request: Request,
    input: ByteReadChannel,
    output: ByteWriteChannel,
    engineDispatcher: CoroutineContext,
    appDispatcher: CoroutineContext,
    upgraded: CompletableDeferred<Boolean>?,
    remoteAddress: SocketAddress?,
    localAddress: SocketAddress?,
    override val coroutineContext: CoroutineContext
) : BaseApplicationCall(application), ApplicationCallWithContext {

    override val request = CIOApplicationRequest(
        this,
        remoteAddress as? InetSocketAddress,
        localAddress as? InetSocketAddress,
        input,
        _request
    )

    override val response = CIOApplicationResponse(this, output, input, engineDispatcher, appDispatcher, upgraded)

    internal fun release() {
        request.release()
    }

    init {
        putResponseAttribute()
    }
}
