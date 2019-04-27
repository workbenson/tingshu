package com.github.eprendre.tingshu.widget

import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.bumptech.glide.request.RequestOptions
import com.github.eprendre.tingshu.R


@GlideModule
class MyAppGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val requestOptions = RequestOptions()
        requestOptions.placeholder(R.drawable.ic_notification)
        val transitionOptions = DrawableTransitionOptions.withCrossFade(
            DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true)
        )

        builder.setDefaultRequestOptions(requestOptions)
        builder.setDefaultTransitionOptions(Drawable::class.java, transitionOptions)
    }
}
