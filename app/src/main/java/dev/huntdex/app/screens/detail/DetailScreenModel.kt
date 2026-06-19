package dev.huntdex.app.screens.detail

import cafe.adriel.voyager.core.model.ScreenModel
import dev.huntdex.core.navigation.AppNavigator

sealed interface DetailIntent {
    data object GoBack : DetailIntent
}

class DetailScreenModel(
    private val navigator: AppNavigator
) : ScreenModel {
    fun onIntent(intent: DetailIntent) {
        when (intent) {
            is DetailIntent.GoBack -> navigator.navigateBack()
        }
    }
}
