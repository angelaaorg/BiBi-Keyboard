/**
 * IME 面板布局缩放与 inset 协调器。
 *
 * 归属模块：ime
 */
package com.brycewg.asrkb.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import com.brycewg.asrkb.R
import com.brycewg.asrkb.ime.layout.BlockDefRegistry
import com.brycewg.asrkb.ime.layout.KeyboardLayoutPanel
import com.brycewg.asrkb.ime.layout.KeyboardLayoutRuntimeApplier
import com.brycewg.asrkb.ime.layout.KeyboardLayoutStore
import com.brycewg.asrkb.store.Prefs
import com.brycewg.asrkb.store.debug.DebugLogManager

internal class ImeLayoutController(
    private val prefs: Prefs,
    private val themeStyler: ImeThemeStyler,
    private val viewRefsProvider: () -> ImeViewRefs?
) {
    private var rootView: View? = null
    private var systemNavBarBottomInset: Int = 0
    private var lastAppliedHeightScale: Float = 1.0f

    fun installKeyboardInsetsListener(rootView: View) {
        this.rootView = rootView
        themeStyler.installKeyboardInsetsListener(rootView) { bottom ->
            systemNavBarBottomInset = bottom
            applyKeyboardHeightScale()
        }
    }

    fun bindMicVerticalFix(views: ImeViewRefs) {
        views.btnMic?.translationY = 0f
    }

    fun applyKeyboardHeightScale(): Boolean {
        val root = rootView ?: viewRefsProvider()?.rootView ?: return false
        val refs = viewRefsProvider()
        var layoutChanged = false

        val tier = prefs.keyboardHeightTier
        val scale = when (tier) {
            1 -> 0.85f
            3 -> 1.15f
            else -> 1.0f
        }

        fun dp(v: Float): Int {
            val d = root.resources.displayMetrics.density
            return (v * d + 0.5f).toInt()
        }

        // 麦克风现在由容器居中；缩放变化时清掉旧版本可能留下的位移。
        if (kotlin.math.abs(lastAppliedHeightScale - scale) > 1e-3f) {
            lastAppliedHeightScale = scale
            refs?.btnMic?.translationY = 0f
        }

        fun updateLayoutSize(view: View?, width: Int? = null, height: Int? = null) {
            if (view == null) return
            val lp = view.layoutParams ?: return
            var changed = false
            if (width != null && lp.width != width) {
                lp.width = width
                changed = true
            }
            if (height != null && lp.height != height) {
                lp.height = height
                changed = true
            }
            if (changed) {
                view.layoutParams = lp
                layoutChanged = true
            }
        }

        // 同步一次当前 RootWindowInsets，避免首次缩放时 bottom inset 尚未写入导致底部裁剪
        run {
            val rw = ViewCompat.getRootWindowInsets(root)
            var b = if (rw != null) ImeInsetsResolver.resolveBottomInset(rw, root.resources) else 0
            if (b <= 0) {
                val decor = root.rootView
                b = decor.findViewById<View>(android.R.id.navigationBarBackground)?.height ?: 0
            }
            if (b > 0) {
                systemNavBarBottomInset = b
            }
        }

        // 应用底部间距（无论是否缩放都需要）
        val fl = root as? FrameLayout
        if (fl != null) {
            val ps = fl.paddingStart
            val pe = fl.paddingEnd
            val pt = dp(8f * scale)
            val basePb = dp(12f * scale)
            // 添加用户设置的底部间距
            val extraPadding = dp(prefs.keyboardBottomPaddingDp.toFloat())
            // 添加系统导航栏高度以适配 Android 15 边缘到边缘显示
            val pb = basePb + extraPadding + systemNavBarBottomInset
            if (fl.paddingTop != pt || fl.paddingBottom != pb) {
                fl.setPaddingRelative(ps, pt, pe, pb)
                layoutChanged = true
            }
        }

        fun scaleSquareButton(id: Int) {
            val v = root.findViewById<View>(id) ?: return
            updateLayoutSize(v, width = dp(40f * scale), height = dp(40f * scale))
        }

        fun scaleGestureButton(v: View?) {
            val baseSize = 86f * scale
            updateLayoutSize(v, width = dp(baseSize), height = dp(baseSize))
        }

        fun scaleChildrenByTag(root: View?, tag: String, height: Int) {
            if (root == null) return
            if (root is android.view.ViewGroup) {
                for (i in 0 until root.childCount) {
                    scaleChildrenByTag(root.getChildAt(i), tag, height)
                }
            }
            val t = root.tag as? String
            if (t == tag) {
                updateLayoutSize(root, height = height)
            }
        }

        val ids40 = (
            KeyboardLayoutPanel.values().flatMap { panel ->
                BlockDefRegistry.default.defsFor(panel).mapNotNull { it.viewId }
            } +
                listOf(R.id.clip_btnBack, R.id.clip_btnDelete)
            )
            .filter { it != R.id.groupMicStatus }
            .distinct()
        ids40.forEach { scaleSquareButton(it) }
        scaleGestureButton(refs?.btnGestureCancel)
        scaleGestureButton(refs?.btnGestureSend)

        // 缩放中央按钮（仅高度，宽度由约束控制）
        run {
            val v1: View? = refs?.btnExtCenter1 ?: root.findViewById(R.id.btnExtCenter1)
            updateLayoutSize(v1, height = dp(40f * scale))
        }

        run {
            val v2: View? = refs?.btnExtCenter2 ?: root.findViewById(R.id.btnExtCenter2)
            updateLayoutSize(v2, height = dp(40f * scale))
        }

        if (KeyboardLayoutRuntimeApplier.applyAll(root, KeyboardLayoutStore.load(prefs), scale)) {
            layoutChanged = true
        }

        // 数字/符号面板：总高度对齐主键盘画布，通过调整按钮高度避免切换跳变
        run {
            val panel: View? = refs?.layoutNumpadPanel ?: root.findViewById(R.id.layoutNumpadPanel)
            if (panel != null) {
                val canvasHeight = root.findViewById<View>(R.id.keyboardLayoutCanvas)?.height
                    ?.takeIf { it > 0 }
                    ?: refs?.layoutMainKeyboard?.height?.takeIf { it > 0 }
                val targetPanelHeight = canvasHeight ?: dp(190f * scale)
                val gapPx = dp(6f)
                val rowHeightPx = ((targetPanelHeight - gapPx * 3) / 4).coerceAtLeast(dp(32f))
                updateLayoutSize(panel, height = targetPanelHeight)
                scaleChildrenByTag(panel, "key40", rowHeightPx)

                val ps = panel.paddingStart
                val pe = panel.paddingEnd
                val pb = panel.paddingBottom
                if (panel.paddingTop != 0) {
                    panel.setPaddingRelative(ps, 0, pe, pb)
                    layoutChanged = true
                }

                fun updateLinearTopMargin(id: Int, topPx: Int) {
                    val v = panel.findViewById<View>(id) ?: return
                    val lp = v.layoutParams as? LinearLayout.LayoutParams ?: return
                    if (lp.topMargin == topPx) return
                    lp.topMargin = topPx
                    v.layoutParams = lp
                    layoutChanged = true
                }

                fun updateLinearBottomMargin(id: Int, bottomPx: Int) {
                    val v = panel.findViewById<View>(id) ?: return
                    val lp = v.layoutParams as? LinearLayout.LayoutParams ?: return
                    if (lp.bottomMargin == bottomPx) return
                    lp.bottomMargin = bottomPx
                    v.layoutParams = lp
                    layoutChanged = true
                }

                updateLinearBottomMargin(R.id.rowNumpadDigits, gapPx)
                updateLinearBottomMargin(R.id.rowPunct1, gapPx)
                updateLinearBottomMargin(R.id.rowPunct2, gapPx)
                updateLinearTopMargin(R.id.rowNumpadBottomBar, 0)
            }
        }

        // 麦克风容器需要和 AI 面板麦克风同一基线，避免面板切换时出现几像素跳动。
        refs?.groupMicStatus?.let { group ->
            if (group.translationY != 0f) {
                group.translationY = 0f
            }
        }
        refs?.btnMic?.let { mic ->
            val group = refs?.groupMicStatus
            val groupSize = listOfNotNull(
                group?.dimensionOrLayoutParam(isWidth = true),
                group?.dimensionOrLayoutParam(isWidth = false)
            ).minOrNull()
            val size = if (groupSize != null) {
                (groupSize * MIC_SIZE_RATIO).toInt().coerceAtLeast(1)
            } else {
                dp(80f * scale)
            }
            if (mic.customSize != size) {
                mic.customSize = size
                layoutChanged = true
            }
            mic.setMaxImageSize((size * MIC_ICON_SIZE_RATIO).toInt().coerceAtLeast(1))
        }
        // 确保主键盘麦克风仅在主键盘可见时参与层级排序。
        refs
            ?.takeIf { it.layoutMainKeyboard?.visibility == View.VISIBLE }
            ?.groupMicStatus
            ?.bringToFront()
        return layoutChanged
    }

    private fun View.dimensionOrLayoutParam(isWidth: Boolean): Int? {
        val lpValue = if (isWidth) layoutParams?.width else layoutParams?.height
        if (lpValue != null && lpValue > 0) return lpValue
        val current = if (isWidth) width else height
        return current.takeIf { it > 0 }
    }

    private companion object {
        private const val MIC_SIZE_RATIO = 0.9f
        private const val MIC_ICON_SIZE_RATIO = 0.42f
    }

    fun hasResolvedBottomInset(): Boolean = systemNavBarBottomInset > 0

    fun fixImeInsetsIfNeeded(
        imeViewVisible: Boolean,
        outInsets: InputMethodService.Insets,
        decorView: View?
    ) {
        if (!imeViewVisible) return
        val input = rootView ?: viewRefsProvider()?.rootView ?: return
        val decor = decorView ?: return

        val decorH = decor.height
        val decorW = decor.width
        if (decorH <= 0 || decorW <= 0) return

        var inputH = input.height
        if (inputH <= 0) {
            // 视图尚未 layout 时，使用一次 measure 获取 wrap_content 目标高度
            try {
                val wSpec = View.MeasureSpec.makeMeasureSpec(decorW, View.MeasureSpec.EXACTLY)
                val hSpec = View.MeasureSpec.makeMeasureSpec(decorH, View.MeasureSpec.AT_MOST)
                input.measure(wSpec, hSpec)
                inputH = input.measuredHeight
            } catch (t: Throwable) {
                android.util.Log.w("AsrKeyboardService", "fixImeInsets measure failed", t)
                return
            }
        }
        if (inputH <= 0) return

        val beforeContentTop = outInsets.contentTopInsets
        val beforeVisibleTop = outInsets.visibleTopInsets

        val topByHeight = (decorH - inputH).coerceIn(0, decorH)
        var locationTop = -1
        run {
            try {
                val loc = IntArray(2)
                input.getLocationInWindow(loc)
                locationTop = loc[1]
            } catch (t: Throwable) {
                android.util.Log.w(
                    "AsrKeyboardService",
                    "fixImeInsets getLocationInWindow failed",
                    t
                )
            }
        }
        val top = if (locationTop in 0 until decorH) {
            minOf(locationTop, topByHeight)
        } else {
            topByHeight
        }

        val correctionThresholdPx = (decor.resources.displayMetrics.density * 2f + 0.5f).toInt().coerceAtLeast(
            1
        )
        val needsColdStartFix = beforeContentTop <= 0
        // 系统给出的 contentTopInsets 偏大时会导致宿主上移不足，出现输入框被键盘遮挡。
        val needsOverlapFix = beforeContentTop > top + correctionThresholdPx
        val needsFix = top > 0 && decorH > inputH && (needsColdStartFix || needsOverlapFix)
        if (!needsFix) return

        outInsets.contentTopInsets = top
        outInsets.visibleTopInsets = top
        outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_REGION
        // 触摸区域限定为键盘区域，避免空白区域吞触摸
        outInsets.touchableRegion.set(0, top, decorW, decorH)

        DebugLogManager.log(
            category = "ime",
            event = "compute_insets_fix",
            data = mapOf(
                "decorH" to decorH,
                "decorW" to decorW,
                "inputH" to inputH,
                "beforeContentTop" to beforeContentTop,
                "beforeVisibleTop" to beforeVisibleTop,
                "topByHeight" to topByHeight,
                "locationTop" to locationTop,
                "needsColdStartFix" to needsColdStartFix,
                "needsOverlapFix" to needsOverlapFix,
                "correctionThresholdPx" to correctionThresholdPx,
                "afterTop" to top
            )
        )
    }
}
