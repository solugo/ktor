
val serialization_version: String by project.extra

plugins {
    id("kotlinx-serialization")
}

kotlin.sourceSets {
    val commonMain by getting {
        dependencies {
            api(project(":ktor-http"))
            api(project(":ktor-client:ktor-client-core"))
        }
    }

   val jvmTest by getting {
        dependencies {
            api("org.jetbrains.kotlinx:kotlinx-serialization-core:$serialization_version")
            api(project(":ktor-client:ktor-client-plugins:ktor-client-content-negotiation"))
            api(project(":ktor-shared:ktor-shared-serialization-kotlinx"))
        }
    }
}
