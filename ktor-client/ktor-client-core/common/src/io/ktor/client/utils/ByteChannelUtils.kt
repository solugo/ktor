/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.utils

import io.ktor.client.content.*
import io.ktor.utils.io.*
import io.ktor.utils.io.pool.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

internal fun ByteReadChannel.observable(
    context: CoroutineContext,
    contentLength: Long?,
    listener: ProgressListener
) = GlobalScope.writer(context, autoFlush = true) {
    ByteArrayPool.useInstance { byteArray ->
        val total = contentLength ?: -1
        var bytesSend = 0L
        while (!this@observable.isClosedForRead) {
            println("starting read")
            val read = this@observable.readAvailable(byteArray)
            println("starting write")
            channel.writeFully(byteArray, offset = 0, length = read)
            println("read and write succeed $read")
            if (read == -1) {
                println("read is -1, isClosedForRead is ${this@observable.isClosedForRead}")
                continue
            }
            bytesSend += read
            listener(bytesSend, total)
        }
        println("finished writer")
        val closedCause = this@observable.closedCause
        channel.close(closedCause)
        if (closedCause == null && bytesSend == 0L) {
            listener(bytesSend, total)
        }
        println("exiting writer")
    }
}.channel
