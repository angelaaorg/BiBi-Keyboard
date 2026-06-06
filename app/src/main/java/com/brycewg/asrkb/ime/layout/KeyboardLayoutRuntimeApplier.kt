/**
 * 将自定义键盘布局应用到 IME View 树。
 *
 * 归属模块：ime/layout
 */
package com.brycewg.asrkb.ime.layout

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.brycewg.asrkb.R
import com.brycewg.asrkb.ime.ImeKeyboardViewFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton

object KeyboardLayoutRuntimeApplier {
    private const val KEY_MARGIN_DP = 6f

    fun applyAll(root: View, bundle: KeyboardLayoutBundle, scale: Float): Boolean {
        val mainPanelVisible = root.findViewById<View>(R.id.layoutMainKeyboard)?.visibility == View.VISIBLE
        val changedMain = applyPanel(
            root = root,
            container = root.findViewById(R.id.keyboardLayoutCanvas),
            layout = bundle.main,
            scale = scale,
            extraViews = mapOf("mic" to root.findViewById(R.id.groupMicStatus)),
            isPlacedViewVisible = { view -> view.id != R.id.groupMicStatus || mainPanelVisible }
        )
        val changedAi = applyPanel(
            root = root,
            container = root.findViewById(R.id.layoutAiEditPanel),
            layout = bundle.aiEdit,
            scale = scale
        )
        val changedRecording = applyPanel(
            root = root,
            container = root.findViewById(R.id.rowRecordingGestures),
            layout = bundle.recording,
            scale = scale
        )
        if (mainPanelVisible) {
            root.findViewById<View>(R.id.groupMicStatus)?.bringToFront()
        }
        return changedMain || changedAi || changedRecording
    }

    private fun applyPanel(
        root: View,
        container: View?,
        layout: KeyboardLayout,
        scale: Float,
        extraViews: Map<String, View?> = emptyMap(),
        isPlacedViewVisible: (View) -> Boolean = { true }
    ): Boolean {
        val frame = container as? FrameLayout ?: return false
        val registry = BlockDefRegistry.default
        val width = resolveFrameWidth(frame, root) ?: return false
        val margin = dp(root, KEY_MARGIN_DP * scale)
        val cellWidth = width.toFloat() / layout.gridSize.cols.toFloat()
        val cellHeight = cellWidth * scale
        val targetHeight = (cellHeight * layout.gridSize.rows).toInt()
        var changed = setSize(frame, ViewGroup.LayoutParams.MATCH_PARENT, targetHeight)
        changed = resetFrameTopMargin(frame) || changed

        val placedViewIds = mutableSetOf<Int>()
        layout.blocks.forEach { block ->
            val def = registry.get(layout.panel, block.defId) ?: registry.get(block.defId) ?: return@forEach
            val view = extraViews[block.defId]
                ?: def.viewId?.let(root::findViewById)
                ?: findDynamicBlockView(frame, layout.panel, block.defId)
                ?: createDynamicBlockView(frame, layout.panel, def)
                ?: return@forEach
            val placement = block.placement.snapped()
            val left = (placement.col * cellWidth).toInt() + margin / 2
            val top = (placement.row * cellHeight).toInt() + margin / 2
            val blockWidth = (placement.width * cellWidth).toInt() - margin
            val blockHeight = (placement.height * cellHeight).toInt() - margin
            val micBounds = if (view.id == R.id.btnAiPanelMic) {
                centeredMicBounds(left, top, blockWidth, blockHeight)
            } else {
                PlacedBounds(left, top, blockWidth, blockHeight)
            }
            changed = placeView(
                view = view,
                preferredParent = frame,
                left = micBounds.left,
                top = micBounds.top,
                width = micBounds.width,
                height = micBounds.height,
                visible = isPlacedViewVisible(view)
            ) ||
                changed
            def.viewId?.let(placedViewIds::add)
        }

        registry.defsFor(layout.panel).forEach { def ->
            val id = def.viewId ?: return@forEach
            if (id !in placedViewIds && extraViews.values.none { it?.id == id }) {
                root.findViewById<View>(id)?.visibility = View.GONE
            }
        }
        hideUnplacedDynamicBlockViews(frame, layout)
        return changed
    }

    private data class PlacedBounds(
        val left: Int,
        val top: Int,
        val width: Int,
        val height: Int
    )

    private fun centeredMicBounds(left: Int, top: Int, width: Int, height: Int): PlacedBounds {
        val size = (minOf(width, height) * MIC_SIZE_RATIO).toInt().coerceAtLeast(1)
        return PlacedBounds(
            left = left + (width - size) / 2,
            top = top + (height - size) / 2,
            width = size,
            height = size
        )
    }

    private fun resolveFrameWidth(frame: FrameLayout, root: View): Int? {
        frame.width.takeIf { it > 0 }?.let { return it }
        val parentView = frame.parent as? View
        val parentContentWidth = parentView?.let { parent ->
            parent.width - parent.paddingStart - parent.paddingEnd
        } ?: 0
        parentContentWidth.takeIf { it > 0 }?.let { return it }
        val rootContentWidth = root.width - root.paddingStart - root.paddingEnd
        return rootContentWidth.takeIf { it > 0 }
    }

    private fun placeView(
        view: View,
        preferredParent: FrameLayout,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        visible: Boolean
    ): Boolean {
        var changed = false
        if (view.parent == null && view.id != R.id.groupMicStatus) {
            preferredParent.addView(view)
            changed = true
        }
        val targetVisibility = if (visible) View.VISIBLE else View.GONE
        if (view.visibility != targetVisibility) {
            view.visibility = targetVisibility
            changed = true
        }
        val finalWidth = width.coerceAtLeast(1)
        val finalHeight = height.coerceAtLeast(1)
        val lp = (view.layoutParams as? FrameLayout.LayoutParams)
            ?: FrameLayout.LayoutParams(finalWidth, finalHeight)
        if (lp.width != finalWidth || lp.height != finalHeight || lp.leftMargin != left || lp.topMargin != top) {
            lp.width = finalWidth
            lp.height = finalHeight
            lp.leftMargin = left
            lp.topMargin = top
            lp.gravity = Gravity.START or Gravity.TOP
            view.layoutParams = lp
            changed = true
        }
        changed = applyPlacedMicSizing(view, finalWidth, finalHeight) || changed
        return changed
    }

    private fun applyPlacedMicSizing(view: View, width: Int, height: Int): Boolean = when (view.id) {
        R.id.btnAiPanelMic -> applyMicFabSizing(view as? FloatingActionButton, minOf(width, height))
        R.id.groupMicStatus -> {
            val centered = applyMainMicContainerGravity(view)
            val micSize = micContentSize(width, height)
            applyMicFabSizing(view.findViewById<FloatingActionButton>(R.id.btnMic), micSize) || centered
        }
        else -> false
    }

    private fun applyMainMicContainerGravity(view: View): Boolean {
        val group = view as? LinearLayout ?: return false
        if (group.gravity == Gravity.CENTER) return false
        group.gravity = Gravity.CENTER
        return true
    }

    private fun applyMicFabSizing(fab: FloatingActionButton?, size: Int): Boolean {
        if (fab == null) return false
        val finalSize = size.coerceAtLeast(1)
        var changed = false
        if (fab.customSize != finalSize) {
            fab.customSize = finalSize
            changed = true
        }
        val iconSize = (finalSize * MIC_ICON_SIZE_RATIO).toInt().coerceAtLeast(1)
        fab.setMaxImageSize(iconSize)
        return changed
    }

    private fun micContentSize(width: Int, height: Int): Int = (minOf(width, height).coerceAtLeast(1) * MIC_SIZE_RATIO).toInt().coerceAtLeast(1)

    private fun setSize(view: View, width: Int, height: Int): Boolean {
        val lp = view.layoutParams ?: return false
        if (lp.width == width && lp.height == height) return false
        lp.width = width
        lp.height = height
        view.layoutParams = lp
        return true
    }

    private fun resetFrameTopMargin(view: View): Boolean {
        val lp = view.layoutParams as? FrameLayout.LayoutParams ?: return false
        if (lp.topMargin == 0 && lp.leftMargin == 0) return false
        lp.topMargin = 0
        lp.leftMargin = 0
        view.layoutParams = lp
        return true
    }

    private fun dp(view: View, value: Float): Int = (value * view.resources.displayMetrics.density + 0.5f).toInt()

    private fun findDynamicBlockView(frame: FrameLayout, panel: KeyboardLayoutPanel, defId: String): View? {
        for (i in 0 until frame.childCount) {
            val child = frame.getChildAt(i)
            if (KeyboardLayoutViewTags.panelOf(child) == panel &&
                KeyboardLayoutViewTags.defIdOf(child) == defId &&
                KeyboardLayoutViewTags.isDynamicBlockView(child)
            ) {
                return child
            }
        }
        return null
    }

    private fun createDynamicBlockView(frame: FrameLayout, panel: KeyboardLayoutPanel, def: BlockDef): View? {
        if (def.viewId != null) return null
        return ImeKeyboardViewFactory.createLayoutBlockButton(frame.context, panel, def).also { frame.addView(it) }
    }

    private fun hideUnplacedDynamicBlockViews(frame: FrameLayout, layout: KeyboardLayout) {
        val placedDefIds = layout.blocks.map { it.defId }.toSet()
        for (i in 0 until frame.childCount) {
            val child = frame.getChildAt(i)
            if (KeyboardLayoutViewTags.panelOf(child) == layout.panel &&
                KeyboardLayoutViewTags.isDynamicBlockView(child) &&
                KeyboardLayoutViewTags.defIdOf(child) !in placedDefIds
            ) {
                child.visibility = View.GONE
            }
        }
    }

    private const val MIC_SIZE_RATIO = 0.9f
    private const val MIC_ICON_SIZE_RATIO = 0.42f
}
