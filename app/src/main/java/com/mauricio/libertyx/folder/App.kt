package com.mauricio.libertyx.folder

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder

class App : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        instance = this
        com.mauricio.libertyx.folder.pro.Pro.iniciar(this)
        if (BuildConfig.PRO) com.mauricio.libertyx.folder.pro.Loja.iniciar(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()

    companion object {
        lateinit var instance: App
            private set
    }
}
