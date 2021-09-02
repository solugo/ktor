/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

package io.ktor.server.servlet

import io.ktor.server.engine.*
import io.ktor.server.request.*
import java.security.*

/**
 * Returns Java's JAAS Principal
 */
@Suppress("unused")
@OptIn(EngineAPI::class)
public val ApplicationRequest.javaSecurityPrincipal: Principal?
    get() = when (this) {
        is ServletApplicationRequest -> servletRequest.userPrincipal
        else -> null
    }
