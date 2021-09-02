/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.testing

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.engine.*
import io.ktor.server.request.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*

/**
 * Represents a test application request
 *
 * @property method HTTP method to be sent or executed
 * @property uri HTTP url to sent request to or was sent to
 * @property version HTTP version to sent or executed
 * @property protocol HTTP protocol to be used or was used
 */
public class TestApplicationRequest constructor(
    call: TestApplicationCall,
    closeRequest: Boolean,
    var method: HttpMethod = HttpMethod.Get,
    var uri: String = "/",
    var version: String = "HTTP/1.1"
) : BaseApplicationRequest(call), CoroutineScope by call {
    var protocol: String = "http"

    override val local: RequestConnectionPoint = object : RequestConnectionPoint {
        override val uri: String
            get() = this@TestApplicationRequest.uri

        override val method: HttpMethod
            get() = this@TestApplicationRequest.method

        override val scheme: String
            get() = protocol

        override val port: Int
            get() = header(HttpHeaders.Host)?.substringAfter(":", "80")?.toInt() ?: 80

        override val host: String
            get() = header(HttpHeaders.Host)?.substringBefore(":") ?: "localhost"

        override val remoteHost: String
            get() = "localhost"

        override val version: String
            get() = this@TestApplicationRequest.version
    }

    /**
     * Request body channel
     */
    @Volatile
    var bodyChannel: ByteReadChannel = if (closeRequest) ByteReadChannel.Empty else ByteChannel()

    override val queryParameters: Parameters by lazy(LazyThreadSafetyMode.NONE) { parseQueryString(queryString()) }

    override val cookies: RequestCookies = RequestCookies(this)

    private var headersMap: MutableMap<String, MutableList<String>>? = hashMapOf()

    /**
     * Add HTTP request header
     */
    public fun addHeader(name: String, value: String) {
        val map = headersMap ?: throw Exception("Headers were already acquired for this request")
        map.getOrPut(name, { arrayListOf() }).add(value)
    }

    override val headers: Headers by lazy(LazyThreadSafetyMode.NONE) {
        val map = headersMap ?: throw Exception("Headers were already acquired for this request")
        headersMap = null
        Headers.build {
            map.forEach { (name, values) ->
                appendAll(name, values)
            }
        }
    }

    override fun receiveChannel(): ByteReadChannel = bodyChannel
}

/**
 * Set HTTP request body text content
 */
public fun TestApplicationRequest.setBody(value: String) {
    setBody(value.toByteArray())
}

/**
 * Set HTTP request body bytes
 */
public fun TestApplicationRequest.setBody(value: ByteArray) {
    bodyChannel = ByteReadChannel(value)
}

/**
 * Set multipart HTTP request body
 */
public fun TestApplicationRequest.setBody(boundary: String, parts: List<PartData>) {
    bodyChannel = writer(Dispatchers.IO) {
        if (parts.isEmpty()) return@writer

        try {
            append("\r\n\r\n")
            parts.forEach {
                append("--$boundary\r\n")
                for ((key, values) in it.headers.entries()) {
                    append("$key: ${values.joinToString(";")}\r\n")
                }
                append("\r\n")
                append(
                    when (it) {
                        is PartData.FileItem -> {
                            it.provider().asStream().copyTo(channel.toOutputStream())
                            ""
                        }
                        is PartData.BinaryItem -> {
                            it.provider().asStream().copyTo(channel.toOutputStream())
                            ""
                        }
                        is PartData.FormItem -> it.value
                    }
                )
                append("\r\n")
            }

            append("--$boundary--\r\n")
        } finally {
            parts.forEach { it.dispose() }
        }
    }.channel
}

private suspend fun WriterScope.append(str: String, charset: Charset = Charsets.UTF_8) {
    channel.writeFully(str.toByteArray(charset))
}
