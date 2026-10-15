package com.paulohenriquesg.fahrenheit.auth

import com.paulohenriquesg.fahrenheit.api.Me
import com.paulohenriquesg.fahrenheit.api.User

/**
 * What a screen needs to know of the signed-in user before its own data
 * loads (#225): the account type and whether they may change items. The
 * podcast page's loading frame keeps an admin's buttons' places with it.
 * Nothing else of the account is kept.
 */
data class SessionRole(val type: String?, val mayUpdate: Boolean) {
    companion object {
        /** From GET /api/me, read on every launch. */
        fun of(me: Me): SessionRole = SessionRole(me.type, me.permissions?.update == true)

        /** From the sign-in response. */
        @Suppress("UNNECESSARY_SAFE_CALL") // Gson leaves a missing object null.
        fun of(user: User): SessionRole = SessionRole(user.type, user.permissions?.update == true)
    }
}
