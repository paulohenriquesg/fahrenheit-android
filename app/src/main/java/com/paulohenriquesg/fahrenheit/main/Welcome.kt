package com.paulohenriquesg.fahrenheit.main

import kotlin.random.Random

/**
 * What the home screen says when you arrive.
 *
 * One greeting for the life of the screen, not one per recomposition: picked
 * with a seed the caller remembers.
 */
object Welcome {

    private val named = listOf(
        "Hello, %s!",
        "Welcome back, %s",
        "Good to see you, %s",
        "Ready when you are, %s",
        "Where were we, %s?",
        "Your shelves are waiting, %s",
        "Something to listen to, %s?"
    )

    private val anonymous = listOf(
        "Hello!",
        "Welcome back",
        "Good to see you",
        "Ready when you are",
        "Where were we?",
        "Your shelves are waiting",
        "Something to listen to?"
    )

    fun pick(name: String, seed: Int): String {
        val index = Random(seed).nextInt(named.size)
        // A blank username would otherwise greet "Hello, !"
        return if (name.isBlank()) anonymous[index] else named[index].format(name)
    }
}
