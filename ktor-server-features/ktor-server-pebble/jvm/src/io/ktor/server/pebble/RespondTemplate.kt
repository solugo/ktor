/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.pebble

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import java.util.*

/**
 * Respond with the specified [template] passing [model]
 *
 * @see PebbleContent
 */
public suspend fun ApplicationCall.respondTemplate(
    template: String,
    model: Map<String, Any>,
    locale: Locale? = null,
    etag: String? = null,
    contentType: ContentType = ContentType.Text.Html.withCharset(
        Charsets.UTF_8
    )
): Unit = respond(PebbleContent(template, model, locale, etag, contentType))
