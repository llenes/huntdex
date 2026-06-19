package dev.huntdex.app.di

import dev.huntdex.app.navigation.VoyagerNavigatorAdapter
import dev.huntdex.app.screens.detail.DetailScreenModel
import dev.huntdex.app.screens.home.HomeScreenModel
import dev.huntdex.core.navigation.AppNavigator
import org.koin.dsl.module

fun appModule(navigatorAdapter: VoyagerNavigatorAdapter) = module {
    single<AppNavigator> { navigatorAdapter }
    factory { HomeScreenModel(get()) }
    factory { DetailScreenModel(get()) }
}
