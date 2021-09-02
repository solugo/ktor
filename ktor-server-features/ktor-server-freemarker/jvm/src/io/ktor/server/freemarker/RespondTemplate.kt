/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.freemarker

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*

/**
 * Respond with the specified [template] passing [model]
 *
 * @see FreeMarkerContent
 */
public suspend fun ApplicationCall.respondTemplate(
    template: String,
    model: Any? = null,
    etag: String? = null,
    contentType: ContentType = ContentType.Text.Html.withCharset(Charsets.UTF_8)
): Unit = respond(FreeMarkerContent(template, model, etag, contentType))
