package dev.huntdex.shared.di

import dev.huntdex.shared.navigation.IosNavigatorAdapter
import dev.huntdex.shared.screens.IosDetailScreenModel
import dev.huntdex.shared.screens.IosHomeScreenModel
import dev.huntdex.core.navigation.AppNavigator
import org.koin.dsl.module

fun iosAppModule(adapter: IosNavigatorAdapter) = module {
    single<AppNavigator> { adapter }
    factory { IosHomeScreenModel(get()) }
    factory { IosDetailScreenModel(get()) }
}
