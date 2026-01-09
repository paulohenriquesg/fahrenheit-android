package com.paulohenriquesg.fahrenheit.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler

object LayoutManager {
    private var _isRowLayout = mutableStateOf(true)
    val isRowLayout = _isRowLayout

    fun initialize(context: Context) {
        val sharedPreferencesHandler = SharedPreferencesHandler(context)
        _isRowLayout.value = sharedPreferencesHandler.getUserPreferences().isRowLayout
    }

    fun toggleLayout(context: Context) {
        _isRowLayout.value = !_isRowLayout.value
        val sharedPreferencesHandler = SharedPreferencesHandler(context)
        sharedPreferencesHandler.saveUserPreferences(
            sharedPreferencesHandler.getUserPreferences().copy(isRowLayout = _isRowLayout.value)
        )
    }

    fun setLayout(context: Context, isRow: Boolean) {
        _isRowLayout.value = isRow
        val sharedPreferencesHandler = SharedPreferencesHandler(context)
        sharedPreferencesHandler.saveUserPreferences(
            sharedPreferencesHandler.getUserPreferences().copy(isRowLayout = isRow)
        )
    }
}
