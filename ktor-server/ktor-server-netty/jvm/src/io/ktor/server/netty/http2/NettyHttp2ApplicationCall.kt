/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.netty.http2

import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.netty.channel.*
import io.netty.handler.codec.http2.*
import kotlin.coroutines.*

internal class NettyHttp2ApplicationCall(
    application: Application,
    context: ChannelHandlerContext,
    val headers: Http2Headers,
    handler: NettyHttp2Handler,
    engineContext: CoroutineContext,
    userContext: CoroutineContext
) : NettyApplicationCall(application, context, headers) {

    override val request = NettyHttp2ApplicationRequest(this, engineContext, context, headers)
    override val response = NettyHttp2ApplicationResponse(this, handler, context, engineContext, userContext)

    init {
        putResponseAttribute()
    }
}
