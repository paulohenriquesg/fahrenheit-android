package com.paulohenriquesg.fahrenheit.main

import com.paulohenriquesg.fahrenheit.api.Library

object LibraryChoice {

    /**
     * Which library to open: the one chosen last time if it is still on the
     * server, otherwise the first. A library can be deleted or renamed between
     * sessions, and a saved id that no longer exists must not leave the screen
     * with nothing selected.
     */
    fun pick(libraries: List<Library>, savedId: String?): Library? =
        libraries.firstOrNull { it.id == savedId } ?: libraries.firstOrNull()
}
