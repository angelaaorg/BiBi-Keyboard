/**
 * 悬浮球 View 树工厂，替代旧 floating_asr_ball.xml 布局。
 *
 * 归属模块：ui/floatingball
 */
package com.brycewg.asrkb.ui.floatingball

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.brycewg.asrkb.R
import com.brycewg.asrkb.store.Prefs
import com.brycewg.asrkb.ui.BibiViewThemes

internal object FloatingBallComposeViewFactory {

    fun create(context: Context, prefs: Prefs): View = FrameLayout(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        addView(createBallContainer(context))
        applyTheme(this, prefs)
    }

    fun applyTheme(root: View, prefs: Prefs) {
        val context = root.context
        val theme = BibiViewThemes.resolve(context, prefs)
        root.findViewById<ImageView>(R.id.edgeHandleIcon)?.imageTintList =
            ColorStateList.valueOf(theme.floatingIcon)
        root.findViewById<ImageView>(R.id.ballIcon)?.imageTintList =
            ColorStateList.valueOf(theme.floatingIcon)
    }

    private fun createBallContainer(context: Context): View = FrameLayout(context).apply {
        id = R.id.ballContainer
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        addView(createRippleClip(context))
        addView(createEdgeHandleIcon(context))
        addView(createBallIcon(context))
    }

    private fun createRippleClip(context: Context): View = FrameLayout(context).apply {
        id = R.id.rippleClip
        clipToOutline = true
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        addView(createRippleView(context, R.id.ripple1))
        addView(createRippleView(context, R.id.ripple2))
        addView(createRippleView(context, R.id.ripple3))
    }

    private fun createRippleView(context: Context, viewId: Int): View = View(context).apply {
        id = viewId
        alpha = 0f
        visibility = View.INVISIBLE
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        )
    }

    private fun createEdgeHandleIcon(context: Context): View = ImageView(context).apply {
        id = R.id.edgeHandleIcon
        alpha = 0f
        visibility = View.GONE
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = context.getString(R.string.cd_floating_edge_handle)
        scaleType = ImageView.ScaleType.FIT_CENTER
        setImageResource(R.drawable.angle_bracket_right)
        layoutParams = FrameLayout.LayoutParams(dp(context, 24), dp(context, 24)).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }
    }

    private fun createBallIcon(context: Context): View = ImageView(context).apply {
        id = R.id.ballIcon
        contentDescription = context.getString(R.string.cd_floating_asr)
        scaleType = ImageView.ScaleType.FIT_CENTER
        setImageResource(R.drawable.microphone_floatingball)
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        )
    }

    private fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density + 0.5f).toInt()
}
