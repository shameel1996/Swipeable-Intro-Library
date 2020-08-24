package com.shameel.swipeableintrolibrary.listeners

import android.animation.Animator
import android.animation.Animator.AnimatorListener


abstract class AnimatorEndListener : AnimatorListener {
    override fun onAnimationStart(animation: Animator) { //do nothing
    }

    override fun onAnimationCancel(animation: Animator) { //do nothing
    }

    override fun onAnimationRepeat(animation: Animator) { //do nothing
    }
}