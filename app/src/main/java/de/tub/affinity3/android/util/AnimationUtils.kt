package de.tub.affinity3.android.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View

fun View.fadeIn(duration: Int) {
    // Set the content view to 0% opacity but visible, so that it is visible
    // (but fully transparent) during the animation.
    alpha = 0f
    visibility = View.VISIBLE

    // Animate the content view to 100% opacity, and clear any animation
    // listener set on the view.
    animate()
        .alpha(1f)
        .setDuration(duration.toLong())
        .setListener(null)
}

fun View.fadeOut(duration: Int) {
    // Animate the loading view to 0% opacity. After the animation ends,
    // set its visibility to GONE as an optimization step (it won't
    // participate in layout passes, etc.)

    animate()
        .alpha(0f)
        .setDuration(duration.toLong())
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                visibility = View.GONE
            }
        })
}
