/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.cio

import io.ktor.http.*
import io.ktor.http.cio.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.request.*
import io.ktor.util.*
import io.ktor.utils.io.*
import java.net.*

internal class CIOApplicationRequest(
    call: ApplicationCall,
    private val remoteAddress: InetSocketAddress?,
    private val localAddress: InetSocketAddress?,
    private val input: ByteReadChannel,
    private val request: Request
) : BaseApplicationRequest(call) {
    override val cookies: RequestCookies by lazy(LazyThreadSafetyMode.NONE) { RequestCookies(this) }

    override fun receiveChannel() = input

    @OptIn(InternalAPI::class)
    override val headers: Headers = CIOHeaders(request.headers)

    override val queryParameters: Parameters by lazy(LazyThreadSafetyMode.NONE) {
        val uri = request.uri
        val qIdx = uri.indexOf('?')
        if (qIdx == -1 || qIdx == uri.lastIndex) return@lazy Parameters.Empty

        parseQueryString(uri.substring(qIdx + 1))
    }

    override val local: RequestConnectionPoint = object : RequestConnectionPoint {
        override val scheme: String
            get() = "http"

        override val version: String
            get() = request.version.toString()

        override val uri: String
            get() = request.uri.toString()

        override val host: String
            get() = localAddress?.let { it.hostName ?: it.address.hostAddress }
                ?: request.headers["Host"]?.toString()?.substringBefore(":")
                ?: "localhost"

        override val port: Int
            get() = localAddress?.port
                ?: request.headers["Host"]?.toString()
                    ?.substringAfter(":", "80")?.toInt()
                ?: 80

        override val method: HttpMethod
            get() = HttpMethod.parse(request.method.value)

        override val remoteHost: String
            get() = remoteAddress?.let {
                it.hostName ?: it.address.hostAddress
            } ?: "unknown"
    }

    internal fun release() {
        request.release()
    }
}
