/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package io.ktor.client.tests.utils.tests

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

internal fun Application.eventsTest() {
    routing {
        route("events") {
            get("basic") {
                call.respondText("Hello, world!".repeat(1000))
            }
            get("redirect") {
                call.respondRedirect("basic")
            }
            get("cache") {
                call.response.cacheControl(CacheControl.MaxAge(60))
                call.respondText("Hello, world!")
            }
        }
    }
}
