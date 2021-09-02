/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.plugins.auth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.tests.utils.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlin.test.*

class AuthTest : ClientLoader() {

    @OptIn(InternalAPI::class)
    @Test
    fun testDigestAuthLegacy() = clientTests(listOf("Js")) {
        config {
            install(Auth) {
                digest {
                    credentials {
                        DigestAuthCredentials("MyName", "Circle Of Life")
                    }

                    realm = "testrealm@host.com"
                }
            }
        }
        test { client ->
            client.prepareGet("$TEST_SERVER/auth/digest").execute {
                assertTrue(it.status.isSuccess())
            }
        }
    }

    @Test
    fun testDigestAuth() = clientTests(listOf("Js")) {
        config {
            install(Auth) {
                digest {
                    credentials { DigestAuthCredentials("MyName", "Circle Of Life") }
                    realm = "testrealm@host.com"
                }
            }
        }
        test { client ->
            client.get("$TEST_SERVER/auth/digest").let {
                assertTrue(it.status.isSuccess())
            }
        }
    }

    @Test
    fun testDigestAuthPerRealm() = clientTests(listOf("Js")) {
        config {
            install(Auth) {
                digest {
                    credentials { DigestAuthCredentials("MyName", "Circle Of Life") }
                    realm = "testrealm@host.com"
                }
                digest {
                    credentials { DigestAuthCredentials("MyName", "some password") }
                    realm = "testrealm-2@host.com"
                }
            }
        }
        test { client ->
            client.get("$TEST_SERVER/auth/digest").let {
                assertTrue(it.status.isSuccess())
            }
            client.get("$TEST_SERVER/auth/digest-2").let {
                assertTrue(it.status.isSuccess())
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun testBasicAuthLegacy() = clientTests(listOf("Js")) {
        config {
            install(Auth) {
                basic {
                    username = "MyUser"
                    password = "1234"
                }
            }
        }

        test { client ->
            client.get("$TEST_SERVER/auth/basic-fixed").body<String>()
        }
    }

    @Test
    fun testBasicAuth() = clientTests(listOf("Js")) {
        config {
            install(Auth) {
                basic {
                    credentials { BasicAuthCredentials("MyUser", "1234") }
                }
            }
        }

        test { client ->
            client.get("$TEST_SERVER/auth/basic-fixed")
        }
    }

    @Test
    fun testBasicAuthWithoutNegotiationLegacy() = clientTests {
        config {
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials("MyUser", "1234")
                    }

                    sendWithoutRequest { true }
                }
            }
        }

        test { client ->
            client.get("$TEST_SERVER/auth/basic-fixed").body<String>()
        }
    }

    @Test
    fun testBasicAuthWithoutNegotiation() = clientTests {
        config {
            install(Auth) {
                basic {
                    credentials { BasicAuthCredentials("MyUser", "1234") }
                    sendWithoutRequest { true }
                }
            }
        }

        test { client ->
            client.get("$TEST_SERVER/auth/basic-fixed")
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun testUnauthorizedBasicAuthLegacy() = clientTests(listOf("Js")) {
        config {
            install(Auth) {
                basic {
                    username = "usr"
                    password = "pw"
                }
            }
            expectSuccess = false
        }

        test { client ->
            client.prepareGet("$TEST_SERVER/auth/unauthorized").execute { response ->
                assertEquals(HttpStatusCode.Unauthorized, response.status)
            }
        }
    }

    @Test
    fun testUnauthorizedBasicAuth() = clientTests(listOf("Js")) {
        config {
            install(Auth) {
                basic {
                    credentials { BasicAuthCredentials("usr", "pw") }
                }
            }
            expectSuccess = false
        }

        test { client ->
            val response = client.get("$TEST_SERVER/auth/unauthorized")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @OptIn(InternalAPI::class)
    @Test
    fun testBasicAuthMultiple() = clientTests(listOf("Js")) {
        config {
            install(Auth) {
                basic {
                    credentials { BasicAuthCredentials("MyUser", "1234") }
                    sendWithoutRequest { it.url.encodedPath.endsWith("basic-fixed") }
                }
                basic {
                    credentials { BasicAuthCredentials("user1", "Password1") }
                    sendWithoutRequest { it.url.encodedPath.endsWith("basic") }
                }
            }
        }

        test { client ->
            client.get("$TEST_SERVER/auth/basic-fixed").bodyAsText()
            client.post("$TEST_SERVER/auth/basic") {
                setBody("{\"test\":\"text\"}")
            }.bodyAsText()
        }
    }

    @Test
    fun testBasicAuthMultipleNotSendWithoutRequest() = clientTests(listOf("Js")) {
        config {
            install(Auth) {
                basic {
                    credentials { BasicAuthCredentials("MyUser", "1234") }
                    realm = "Ktor Server"
                }
                basic {
                    credentials { BasicAuthCredentials("user1", "Password1") }
                    realm = "my-server"
                }
            }
        }

        test { client ->
            client.get("$TEST_SERVER/auth/basic-fixed").bodyAsText()
            client.post("$TEST_SERVER/auth/basic") {
                setBody("{\"test\":\"text\"}")
            }.bodyAsText()
        }
    }

    @Test
    fun testBasicAuthPerRealm() = clientTests(listOf("Js")) {
        config {
            install(Auth) {
                basic {
                    credentials { BasicAuthCredentials("MyUser", "1234") }
                    realm = "Ktor Server"
                }
            }
        }

        test { client ->
            client.get("$TEST_SERVER/auth/basic-fixed")
            client.post("$TEST_SERVER/auth/basic") { expectSuccess = false }.let {
                assertEquals(HttpStatusCode.Unauthorized, it.status)
            }
        }
    }

    @Test
    fun testUnauthorizedBearerAuthWithInvalidAccessAndRefreshTokensAsNulls() = clientTests {
        config {
            install(Auth) {
                bearer {
                    refreshTokens { null }
                    loadTokens { null }
                }
            }

            expectSuccess = false
        }

        test { client ->
            client.prepareGet("$TEST_SERVER/auth/bearer/test-refresh").execute {
                assertEquals(HttpStatusCode.Unauthorized, it.status)
            }
        }
    }

    @Test
    fun testUnauthorizedBearerAuthWithInvalidAccessAndRefreshTokens() = clientTests {
        config {
            install(Auth) {
                bearer {
                    refreshTokens { BearerTokens("invalid", "refresh") }
                    loadTokens { BearerTokens("invalid", "refresh") }
                }
            }

            expectSuccess = false
        }

        test { client ->

            client.prepareGet("$TEST_SERVER/auth/bearer/test-refresh").execute {
                assertEquals(HttpStatusCode.Unauthorized, it.status)
            }
        }
    }

    @Test
    // The return of refreshTokenFun is null, cause it should not be called at all, if loadTokensFun returns valid tokens
    fun testUnauthorizedBearerAuthWithValidAccessTokenAndInvalidRefreshToken() = clientTests {
        config {
            install(Auth) {
                bearer {
                    refreshTokens { null }
                    loadTokens { BearerTokens("valid", "refresh") }
                }
            }
        }

        test { client ->
            client.prepareGet("$TEST_SERVER/auth/bearer/test-refresh").execute {
                assertEquals(HttpStatusCode.OK, it.status)
            }
        }
    }

    @Test
    fun testUnauthorizedBearerAuthWithInvalidAccessTokenAndValidRefreshToken() = clientTests {
        config {
            install(Auth) {
                bearer {
                    refreshTokens { BearerTokens("valid", "refresh") }
                    loadTokens { BearerTokens("invalid", "refresh") }
                    realm = "TestServer"
                }
            }
        }

        test { client ->
            client.prepareGet("$TEST_SERVER/auth/bearer/test-refresh").execute {
                assertEquals(HttpStatusCode.OK, it.status)
            }
        }
    }

    @Suppress("JoinDeclarationAndAssignment")
    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testRefreshWithSameClient() = clientTests {
        test { client ->
            lateinit var clientWithAuth: HttpClient
            clientWithAuth = client.config {
                developmentMode = true

                install(Auth) {
                    bearer {
                        loadTokens {
                            val token = clientWithAuth.get("$TEST_SERVER/auth/bearer/token/first").bodyAsText()
                            BearerTokens(token, token)
                        }

                        refreshTokens {
                            val token = clientWithAuth.get("$TEST_SERVER/auth/bearer/token/second").bodyAsText()
                            BearerTokens(token, token)
                        }
                    }
                }
            }

            val first = clientWithAuth.get("$TEST_SERVER/auth/bearer/first").bodyAsText()
            val second = clientWithAuth.get("$TEST_SERVER/auth/bearer/second").bodyAsText()

            assertEquals("OK", first)
            assertEquals("OK", second)
        }
    }
}
