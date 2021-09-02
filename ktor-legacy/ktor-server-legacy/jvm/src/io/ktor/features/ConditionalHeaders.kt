/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("DEPRECATION_ERROR")

package io.ktor.features

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.util.*
import io.ktor.util.pipeline.*

@Deprecated(
    message = "Moved to io.ktor.server.plugins",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("ConditionalHeaders)", "io.ktor.server.plugins.*")
)
public class ConditionalHeaders(private val versionProviders: List<suspend (OutgoingContent) -> List<Version>>)
