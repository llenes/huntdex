package dev.huntdex.app

import android.app.Application
import dev.huntdex.core.data.di.androidDataModule
import dev.huntdex.core.data.di.dataModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class HuntdexApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@HuntdexApplication)
            modules(dataModule, androidDataModule)
        }
    }
}
