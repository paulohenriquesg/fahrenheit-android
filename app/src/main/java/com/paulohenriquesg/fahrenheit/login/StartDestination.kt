package com.paulohenriquesg.fahrenheit.login

/**
 * Where the app opens: straight in, or at the login form.
 *
 * It takes a session to skip the form, not just an address. Deciding on the
 * host alone made the two activities bounce off each other after a sign-out -
 * login saw a host and went to main, main found no token and came back - until
 * the process died (#63).
 */
enum class StartDestination {
    Login,
    Main;

    companion object {
        fun of(host: String, token: String): StartDestination =
            if (host.isNotEmpty() && token.isNotEmpty()) Main else Login
    }
}
