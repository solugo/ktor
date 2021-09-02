/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

val gson_version: String by project.extra

kotlin.sourceSets {
    val jvmMain by getting {
        dependencies {
            api(project(":ktor-client:ktor-client-plugins:ktor-client-json"))
            api("com.google.code.gson:gson:$gson_version")
        }
    }
    val jvmTest by getting {
        dependencies {
            api(project(":ktor-client:ktor-client-cio"))
            api(project(":ktor-client:ktor-client-plugins:ktor-client-json:ktor-client-json-tests"))
            api(project(":ktor-shared:ktor-shared-serialization-gson"))
        }
    }
}
