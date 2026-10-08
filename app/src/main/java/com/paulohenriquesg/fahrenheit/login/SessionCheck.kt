package com.paulohenriquesg.fahrenheit.login

import retrofit2.HttpException
import java.io.IOException

/**
 * Whether the stored session still works, asked once at launch (#191).
 *
 * @param probe any authenticated request. Through the app's client, an
 *   expired access token is refreshed on the way, so by the time this says
 *   [Result.Ready] Home's first requests carry a token the server accepts.
 */
class SessionCheck(private val probe: suspend () -> Unit) {
    sealed interface Result {
        data object Ready : Result

        /** The sign-in form, with what to say beside it, if anything. */
        data class SignIn(val error: LoginError?) : Result
    }

    suspend fun run(): Result = try {
        probe()
        Result.Ready
    } catch (e: HttpException) {
        // 401 after the refresh token was spent too: Welcome back asks for
        // the password anyway, and blaming one the user has not typed is wrong.
        if (e.code() == 401 || e.code() == 403) Result.SignIn(null) else Result.SignIn(LoginError.ServerError(e.code()))
    } catch (e: IOException) {
        Result.SignIn(LoginError.Unreachable)
    } catch (e: Exception) {
        Result.SignIn(LoginError.Unexpected)
    }
}
