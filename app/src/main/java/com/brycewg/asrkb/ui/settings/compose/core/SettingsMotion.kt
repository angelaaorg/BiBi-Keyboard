/**
 * 设置页 Compose 动效参数。
 *
 * 归属模块：ui/settings/compose/core
 */
package com.brycewg.asrkb.ui.settings.compose.core

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

internal object SettingsMotion {
    const val RouteSlideMillis = 500
    const val RouteReplaceFadeInMillis = 200
    const val RouteReplaceFadeOutMillis = 160
    const val RouteBackgroundOffsetDivisor = 4
    const val RouteForegroundZIndex = 1f
    const val RouteBackgroundZIndex = -1f
    const val PagerBaseMillis = 100
    const val PagerMillisPerPage = 100
    const val PagerMinimumDistancePages = 2

    private val routeEasing = SettingsNavTransitionEasing(0.8f, 0.95f)

    fun routeEnterSpatialSpec(): FiniteAnimationSpec<IntOffset> = tween(
        durationMillis = RouteSlideMillis,
        easing = routeEasing
    )

    fun routeExitSpatialSpec(): FiniteAnimationSpec<IntOffset> = tween(
        durationMillis = RouteSlideMillis,
        easing = routeEasing
    )

    fun pagerSpec(distancePages: Int): FiniteAnimationSpec<Float> = tween(
        durationMillis = PagerBaseMillis +
            PagerMillisPerPage * distancePages.coerceAtLeast(PagerMinimumDistancePages),
        easing = EaseInOut
    )
}

private class SettingsNavTransitionEasing(
    response: Float,
    damping: Float
) : Easing {
    private val decayRate: Float
    private val angularFrequency: Float
    private val phaseRatio: Float

    init {
        val omega = 2.0 * PI / response
        val stiffness = omega * omega
        val dampingCoefficient = damping * 4.0 * PI / response
        angularFrequency = (sqrt(4.0 * stiffness - dampingCoefficient * dampingCoefficient) / 2.0).toFloat()
        decayRate = (-dampingCoefficient / 2.0).toFloat()
        phaseRatio = decayRate / angularFrequency
    }

    override fun transform(fraction: Float): Float {
        val time = fraction.toDouble()
        val decay = exp(decayRate * time)
        return (decay * (-cos(angularFrequency * time) + phaseRatio * sin(angularFrequency * time)) + 1.0).toFloat()
    }
}
