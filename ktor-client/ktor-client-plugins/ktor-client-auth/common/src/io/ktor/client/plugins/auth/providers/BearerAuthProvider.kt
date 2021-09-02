/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.plugins.auth.providers

import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.auth.*
import kotlin.jvm.*

/**
 * Add [BearerAuthProvider] to client [Auth] providers.
 */
public fun Auth.bearer(block: BearerAuthConfig.() -> Unit) {
    with(BearerAuthConfig().apply(block)) {
        providers.add(BearerAuthProvider(_refreshTokens, _loadTokens, _sendWithoutRequest, realm))
    }
}

public class BearerTokens(
    public val accessToken: String,
    public val refreshToken: String
)

/**
 * Parameters that will be passed to [BearerAuthConfig.refreshTokens] lambda
 */
public class RefreshTokensParams(
    public val client: HttpClient,
    public val response: HttpResponse,
    public val oldTokens: BearerTokens?
)

/**
 * [BearerAuthProvider] configuration.
 */
public class BearerAuthConfig {
    internal var _refreshTokens: suspend RefreshTokensParams.() -> BearerTokens? = { null }
    internal var _loadTokens: suspend () -> BearerTokens? = { null }
    internal var _sendWithoutRequest: (HttpRequestBuilder) -> Boolean = { true }

    public var realm: String? = null

    public fun refreshTokens(block: suspend RefreshTokensParams.() -> BearerTokens?) {
        _refreshTokens = block
    }

    public fun loadTokens(block: suspend () -> BearerTokens?) {
        _loadTokens = block
    }

    /**
     * Send credentials in without waiting for [HttpStatusCode.Unauthorized].
     */
    public fun sendWithoutRequest(block: (HttpRequestBuilder) -> Boolean) {
        _sendWithoutRequest = block
    }
}

/**
 * Client bearer [AuthProvider].
 */
public class BearerAuthProvider(
    private val refreshTokens: suspend RefreshTokensParams.() -> BearerTokens?,
    loadTokens: suspend () -> BearerTokens?,
    private val sendWithoutRequestCallback: (HttpRequestBuilder) -> Boolean = { true },
    private val realm: String?
) : AuthProvider {

    override val sendWithoutRequest: Boolean
        get() = error("Deprecated")

    private val tokensHolder = AuthTokenHolder(loadTokens)

    override fun sendWithoutRequest(request: HttpRequestBuilder): Boolean = sendWithoutRequestCallback(request)

    /**
     * Check if current provider is applicable to the request.
     */
    override fun isApplicable(auth: HttpAuthHeader): Boolean {
        if (auth.authScheme != AuthScheme.Bearer) return false
        if (realm == null) return true
        if (auth !is HttpAuthHeader.Parameterized) return false

        return auth.parameter("realm") == realm
    }

    /**
     * Add authentication method headers and creds.
     */
    override suspend fun addRequestHeaders(request: HttpRequestBuilder) {
        val token = tokensHolder.loadToken() ?: return

        request.headers {
            val tokenValue = "Bearer ${token.accessToken}"
            if (contains(HttpHeaders.Authorization)) {
                remove(HttpHeaders.Authorization)
            }
            append(HttpHeaders.Authorization, tokenValue)
        }
    }

    public override suspend fun refreshToken(response: HttpResponse): Boolean {
        val newToken = tokensHolder.setToken {
            refreshTokens(RefreshTokensParams(response.call.client!!, response, tokensHolder.loadToken()))
        }
        return newToken != null
    }

    public suspend fun clearToken() {
        tokensHolder.clearToken()
    }
}
