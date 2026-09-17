package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

/**
 * Response of the unauthenticated `GET /status`.
 *
 * Every field is nullable on purpose. This is the first call made against a host
 * the user just typed, so it has to survive an old server, a differently
 * configured one, or something that is not Audiobookshelf at all - issue #1 was
 * a login broken by a field whose real type did not match the model.
 */
data class ServerStatus(
    @SerializedName("app") val app: String? = null,
    @SerializedName("serverVersion") val serverVersion: String? = null,
    @SerializedName("isInit") val isInit: Boolean? = null,
    @SerializedName("authMethods") val authMethods: List<String>? = null,
    @SerializedName("authFormData") val authFormData: AuthFormData? = null
) {
    /**
     * Servers older than the authMethods field only ever did password sign-in,
     * so absence means local rather than "nothing works".
     */
    val supportsLocal: Boolean
        get() = authMethods?.contains(LOCAL) ?: true

    val supportsOpenId: Boolean
        get() = authMethods?.contains(OPENID) == true

    /**
     * False when the server does not offer password sign-in at all. Showing a
     * required password field to such a user locks them out entirely - the
     * complaint in issue #2.
     */
    val requiresPassword: Boolean
        get() = supportsLocal

    val openIdButtonText: String?
        get() = authFormData?.openIdButtonText

    private companion object {
        const val LOCAL = "local"
        const val OPENID = "openid"
    }
}

data class AuthFormData(
    @SerializedName("authLoginCustomMessage") val loginCustomMessage: String? = null,
    @SerializedName("authOpenIDButtonText") val openIdButtonText: String? = null,
    @SerializedName("authOpenIDAutoLaunch") val openIdAutoLaunch: Boolean? = null
)
