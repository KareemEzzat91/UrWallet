package com.example.urwallet.core.designsystem

import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.example.urwallet.R

object SkeletonLoaderHelper {

    fun start(view: View) {
        if (view.animation == null) {
            val animation = AnimationUtils.loadAnimation(view.context, R.anim.anim_skeleton_pulse)
            view.startAnimation(animation)
        }
    }

    fun stop(view: View) {
        view.clearAnimation()
    }
}

fun View.startSkeletonShimmer() = SkeletonLoaderHelper.start(this)
fun View.stopSkeletonShimmer() = SkeletonLoaderHelper.stop(this)
