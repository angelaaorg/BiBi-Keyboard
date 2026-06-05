/**
 * 输入法主键盘 View 树工厂，替代旧 keyboard_view.xml 主布局。
 *
 * 归属模块：ime
 */
package com.brycewg.asrkb.ime

import android.animation.AnimatorInflater
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.brycewg.asrkb.R
import com.brycewg.asrkb.UiColorTokens
import com.brycewg.asrkb.UiColors
import com.brycewg.asrkb.store.Prefs
import com.brycewg.asrkb.ui.BibiViewThemes
import com.brycewg.asrkb.ui.widgets.PunctKeyView
import com.brycewg.asrkb.ui.widgets.WaveformView
import com.google.android.material.floatingactionbutton.FloatingActionButton

internal object ImeKeyboardViewFactory {

    fun create(context: Context, prefs: Prefs): View {
        val root = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            clipToPadding = true
            clipChildren = true
            background = ContextCompat.getDrawable(context, R.drawable.bg_keyboard_container)
            setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 12))
        }

        root.addView(createMainKeyboard(context))
        root.addView(createAiEditPanel(context))
        root.addView(createNumpadPanel(context))
        root.addView(createClipboardPanel(context))
        root.addView(createMicStatusGroup(context))
        applyTheme(root, prefs)
        return root
    }

    fun applyTheme(root: View, prefs: Prefs) {
        val context = root.context
        val theme = BibiViewThemes.resolve(context, prefs)
        root.setBackgroundColor(theme.keyboardBackground)
        root.findViewById<View>(R.id.layoutClipboardPanel)?.setBackgroundColor(theme.keyboardBackground)

        iconKeyIds.forEach { id ->
            root.findViewById<ImageButton>(id)?.apply {
                background = BibiViewThemes.roundedRipple(
                    context,
                    theme.keyBackground,
                    theme.ripple,
                    theme.iconKeyRadiusDp,
                    insetDp = theme.keyInsetDp
                )
                imageTintList = ColorStateList.valueOf(theme.keyContent)
            }
        }
        rectKeyIds.forEach { id ->
            root.findViewById<View>(id)?.applyRectKeyTheme(theme)
        }
        applyTaggedKeys(root, theme)

        listOf(R.id.btnPunct2, R.id.btnPunct3).forEach { id ->
            root.findViewById<PunctKeyView>(id)?.apply {
                background = BibiViewThemes.roundedRipple(
                    context,
                    theme.keyBackground,
                    theme.ripple,
                    theme.iconKeyRadiusDp,
                    insetDp = theme.keyInsetDp
                )
                setKeyTextColor(theme.keyContent)
            }
        }

        root.findViewById<FloatingActionButton>(R.id.btnMic)?.apply {
            backgroundTintList = ColorStateList.valueOf(theme.micContainer)
            imageTintList = ColorStateList.valueOf(theme.micContent)
        }
        root.findViewById<WaveformView>(R.id.waveformView)?.setWaveformColor(theme.primary)

        statusTextIds.forEach { id ->
            root.findViewById<TextView>(id)?.setTextColor(theme.panelSummary)
        }
        root.findViewById<TextView>(R.id.txtStatusText)?.setTextColor(theme.panelContent)
        root.findViewById<TextView>(R.id.txtAiEditInfo)?.setTextColor(theme.panelContent)
        root.findViewById<TextView>(R.id.clip_txtCount)?.setTextColor(theme.panelSummary)
    }

    private fun createMainKeyboard(context: Context): View {
        val container = FrameLayout(context).apply {
            id = R.id.layoutMainKeyboard
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        column.addView(createExtensionRow(context))
        column.addView(createTopRow(context))
        column.addView(createPunctuationRow(context))

        container.addView(column)
        container.addView(createOverlayRow(context))
        container.addView(createRecordingGestureRow(context))
        return container
    }

    private fun createExtensionRow(context: Context): View {
        val row = ConstraintLayout(context).apply {
            id = R.id.rowExtension
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 50)
            )
        }

        row.addView(
            imageButton(context, R.id.btnExt1, R.string.desc_extension_button_1).withConstraints {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginEnd = dp(context, 6)
            }
        )
        row.addView(
            imageButton(context, R.id.btnExt2, R.string.desc_extension_button_2).withConstraints {
                startToEnd = R.id.btnExt1
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginStart = dp(context, 6)
            }
        )
        row.addView(
            imageButton(context, R.id.btnExt4, R.string.desc_extension_button_4).withConstraints {
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginStart = dp(context, 6)
            }
        )
        row.addView(
            imageButton(context, R.id.btnExt3, R.string.desc_extension_button_3).withConstraints {
                endToStart = R.id.btnExt4
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginEnd = dp(context, 6)
            }
        )
        row.addView(createStatusCenter(context))
        return row
    }

    private fun createStatusCenter(context: Context): View = keyFrame(context, R.id.btnExtCenter1).apply {
        contentDescription = context.getString(R.string.desc_extension_center_button_1)
        isClickable = true
        isFocusable = true
        clipChildren = true
        clipToPadding = true
        layoutParams = ConstraintLayout.LayoutParams(0, dp(context, 40)).apply {
            startToEnd = R.id.btnExt2
            endToStart = R.id.btnExt3
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            marginStart = dp(context, 6)
            marginEnd = dp(context, 6)
        }
        addView(statusText(context, R.id.txtStatusText))
        addView(
            WaveformView(context).apply {
                id = R.id.waveformView
                visibility = View.GONE
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4))
                }
            }
        )
    }

    private fun createTopRow(context: Context): View {
        val row = ConstraintLayout(context).apply {
            id = R.id.rowTop
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 80)
            )
        }
        row.addView(
            imageButton(context, R.id.btnHide, R.string.ext_btn_clipboard, R.drawable.clipboard_toggle)
                .withConstraints {
                    endToStart = R.id.btnBackspace
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    verticalBias = 0f
                    marginEnd = dp(context, 6)
                }
        )
        row.addView(
            imageButton(context, R.id.btnPostproc, R.string.cd_postproc_toggle, R.drawable.magic_wand)
                .withConstraints {
                    startToEnd = R.id.btnPromptPicker
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    verticalBias = 0f
                    marginStart = dp(context, 6)
                }
        )
        row.addView(
            imageButton(context, R.id.btnBackspace, R.string.cd_backspace, R.drawable.backspace_toggle)
                .withConstraints {
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    verticalBias = 0f
                    marginStart = dp(context, 6)
                }
        )
        row.addView(
            imageButton(context, R.id.btnPromptPicker, R.string.cd_ai_edit, R.drawable.pencil_simple_line)
                .withConstraints {
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    verticalBias = 0f
                    marginEnd = dp(context, 6)
                }
        )
        return row
    }

    private fun createPunctuationRow(context: Context): View {
        val row = ConstraintLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(context, 12)
            }
        }
        row.addView(
            imageButton(context, R.id.btnPunct1, R.string.cd_numpad, R.drawable.numpad_toggle)
                .withConstraints {
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    marginEnd = dp(context, 6)
                }
        )
        row.addView(
            punctButton(context, R.id.btnPunct2, R.string.cd_punct_btn_2).withConstraints {
                startToEnd = R.id.btnPunct1
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginStart = dp(context, 6)
                marginEnd = dp(context, 6)
            }
        )
        row.addView(
            punctButton(context, R.id.btnPunct3, R.string.cd_punct_btn_3).withConstraints {
                endToStart = R.id.btnPunct4
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginStart = dp(context, 6)
                marginEnd = dp(context, 6)
            }
        )
        row.addView(
            imageButton(context, R.id.btnPunct4, R.string.cd_vendor_picker, R.drawable.circles_four_toggle)
                .withConstraints {
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    marginStart = dp(context, 6)
                }
        )
        row.addView(
            keyButton(context, R.id.btnExtCenter2, R.string.desc_extension_center_button_2).apply {
                text = context.getString(R.string.cd_space)
                layoutParams = ConstraintLayout.LayoutParams(0, dp(context, 40)).apply {
                    startToEnd = R.id.btnPunct2
                    endToStart = R.id.btnPunct3
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    marginStart = dp(context, 6)
                    marginEnd = dp(context, 6)
                }
            }
        )
        return row
    }

    private fun createOverlayRow(context: Context): View {
        val row = ConstraintLayout(context).apply {
            id = R.id.rowOverlay
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP
                topMargin = dp(context, 96)
            }
        }
        row.addView(
            imageButton(context, R.id.btnSettings, R.string.cd_settings, R.drawable.gear_toggle)
                .withConstraints {
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    marginEnd = dp(context, 6)
                }
        )
        row.addView(
            imageButton(context, R.id.btnImeSwitcher, R.string.cd_prompt_picker, R.drawable.article_toggle)
                .withConstraints {
                    startToEnd = R.id.btnSettings
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    marginStart = dp(context, 6)
                }
        )
        row.addView(
            imageButton(context, R.id.btnEnter, R.string.cd_enter, R.drawable.key_return_toggle)
                .withConstraints {
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    marginStart = dp(context, 6)
                }
        )
        row.addView(
            imageButton(context, R.id.btnAiEdit, R.string.cd_switch_ime, R.drawable.keyboard_toggle)
                .withConstraints {
                    startToEnd = R.id.btnImeSwitcher
                    endToStart = R.id.btnEnter
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    horizontalBias = 1f
                    marginEnd = dp(context, 6)
                }
        )
        return row
    }

    private fun createRecordingGestureRow(context: Context): View {
        val row = ConstraintLayout(context).apply {
            id = R.id.rowRecordingGestures
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }
        row.addView(
            gestureButton(context, R.id.btnGestureCancel, R.string.label_recording_gesture_cancel)
                .withConstraints {
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                }
        )
        row.addView(
            gestureButton(context, R.id.btnGestureSend, R.string.label_recording_gesture_send)
                .withConstraints {
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                }
        )
        return row
    }

    private fun createClipboardPanel(context: Context): View = LinearLayout(context).apply {
        id = R.id.layoutClipboardPanel
        orientation = LinearLayout.VERTICAL
        background = ContextCompat.getDrawable(context, R.drawable.bg_keyboard_container)
        setPadding(0, dp(context, 6), 0, 0)
        visibility = View.GONE
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP
        }
        addView(createClipboardHeader(context))
        addView(
            RecyclerView(context).apply {
                id = R.id.clip_list
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                clipToPadding = false
                setPadding(0, dp(context, 4), 0, dp(context, 4))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0
                ).apply {
                    weight = 1f
                }
            }
        )
    }

    private fun createClipboardHeader(context: Context): View {
        val header = ConstraintLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 44)
            )
        }
        header.addView(
            imageButton(context, R.id.clip_btnBack, R.string.cd_clipboard_back, R.drawable.arrow_left_toggle)
                .withConstraints {
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                }
        )
        header.addView(
            TextView(context).apply {
                id = R.id.clip_txtCount
                gravity = Gravity.CENTER
                setTextColor(UiColors.get(context, UiColorTokens.panelFgVariant))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                layoutParams = ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    startToEnd = R.id.clip_btnBack
                    endToStart = R.id.clip_btnDelete
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                }
            }
        )
        header.addView(
            imageButton(context, R.id.clip_btnDelete, R.string.cd_clipboard_delete, R.drawable.trash_toggle)
                .withConstraints {
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    marginStart = dp(context, 6)
                }
        )
        return header
    }

    private fun createAiEditPanel(context: Context): View {
        val panel = ConstraintLayout(context).apply {
            id = R.id.layoutAiEditPanel
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP
            }
        }

        panel.addView(
            keyFrame(context, R.id.aiEditInfoBar).apply {
                layoutParams = ConstraintLayout.LayoutParams(0, dp(context, 40)).apply {
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    topMargin = dp(context, 5)
                }
                addView(statusText(context, R.id.txtAiEditInfo))
            }
        )
        panel.addView(
            Guideline(context).apply {
                id = R.id.guidelineCenter
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    orientation = ConstraintLayout.LayoutParams.VERTICAL
                    guidePercent = 0.5f
                }
            }
        )

        panel.addView(
            aiEditButtonRow(
                context,
                id = R.id.aiEditRow1Left,
                gravity = Gravity.START or Gravity.CENTER_VERTICAL,
                topDp = 50,
                first = imageButton(context, R.id.btnAiPanelBack, R.string.cd_return_main, R.drawable.arrow_u_up_left_toggle),
                second = imageButton(context, R.id.btnAiPanelApplyPreset, R.string.cd_apply_preset_prompt, R.drawable.lightning_toggle)
            ).withConstraints {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToStart = R.id.guidelineCenter
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = dp(context, 50)
            }
        )
        panel.addView(
            aiEditButtonRow(
                context,
                id = R.id.aiEditRow1Right,
                gravity = Gravity.END or Gravity.CENTER_VERTICAL,
                topDp = 50,
                first = imageButton(context, R.id.btnAiPanelSelectAll, R.string.cd_select_all, R.drawable.selection_all_toggle),
                second = imageButton(context, R.id.btnAiPanelUndo, R.string.cd_backspace, R.drawable.backspace_toggle)
            ).withConstraints {
                startToEnd = R.id.guidelineCenter
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = dp(context, 50)
            }
        )
        panel.addView(
            aiEditButtonRow(
                context,
                id = R.id.aiEditRow2Left,
                gravity = Gravity.START or Gravity.CENTER_VERTICAL,
                topDp = 96,
                first = imageButton(context, R.id.btnAiPanelCursorLeft, R.string.cd_cursor_left, R.drawable.arrow_left_toggle),
                second = imageButton(context, R.id.btnAiPanelCursorRight, R.string.cd_cursor_right, R.drawable.arrow_right_toggle)
            ).withConstraints {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToStart = R.id.guidelineCenter
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = dp(context, 96)
            }
        )
        panel.addView(
            aiEditButtonRow(
                context,
                id = R.id.aiEditRow2Right,
                gravity = Gravity.END or Gravity.CENTER_VERTICAL,
                topDp = 96,
                first = imageButton(context, R.id.btnAiPanelCopy, R.string.cd_copy, R.drawable.copy_toggle),
                second = imageButton(context, R.id.btnAiPanelPaste, R.string.cd_paste, R.drawable.selection_background_toggle)
            ).withConstraints {
                startToEnd = R.id.guidelineCenter
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = dp(context, 96)
            }
        )
        panel.addView(
            aiEditButtonRow(
                context,
                id = R.id.aiEditRow3Left,
                gravity = Gravity.START or Gravity.CENTER_VERTICAL,
                topDp = 142,
                first = imageButton(context, R.id.btnAiPanelNumpad, R.string.cd_numpad, R.drawable.numpad_toggle),
                second = imageButton(context, R.id.btnAiPanelSelect, R.string.cd_select_toggle, R.drawable.selection_toggle)
            ).withConstraints {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = dp(context, 142)
                width = ConstraintLayout.LayoutParams.WRAP_CONTENT
            }
        )
        panel.addView(
            keyButton(context, R.id.btnAiPanelSpace, R.string.cd_space).apply {
                text = context.getString(R.string.cd_space)
                layoutParams = ConstraintLayout.LayoutParams(0, dp(context, 40)).apply {
                    startToEnd = R.id.aiEditRow3Left
                    endToStart = R.id.aiEditRow3Right
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    topMargin = dp(context, 142)
                    marginStart = dp(context, 6)
                    marginEnd = dp(context, 6)
                }
            }
        )
        panel.addView(
            aiEditButtonRow(
                context,
                id = R.id.aiEditRow3Right,
                gravity = Gravity.END or Gravity.CENTER_VERTICAL,
                topDp = 142,
                first = imageButton(context, R.id.btnAiPanelMoveStart, R.string.cd_move_home, R.drawable.arrow_line_left_toggle),
                second = imageButton(context, R.id.btnAiPanelMoveEnd, R.string.cd_move_end, R.drawable.arrow_line_right_toggle)
            ).withConstraints {
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = dp(context, 142)
                width = ConstraintLayout.LayoutParams.WRAP_CONTENT
            }
        )
        return panel
    }

    private fun aiEditButtonRow(
        context: Context,
        id: Int,
        gravity: Int,
        topDp: Int,
        first: ImageButton,
        second: ImageButton
    ): LinearLayout = LinearLayout(context).apply {
        this.id = id
        this.gravity = gravity
        orientation = LinearLayout.HORIZONTAL
        layoutParams = ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            topMargin = dp(context, topDp)
        }
        addView(
            first.apply {
                layoutParams = LinearLayout.LayoutParams(dp(context, 40), dp(context, 40)).apply {
                    marginEnd = dp(context, 6)
                }
            }
        )
        addView(
            second.apply {
                layoutParams = LinearLayout.LayoutParams(dp(context, 40), dp(context, 40))
            }
        )
    }

    private fun createNumpadPanel(context: Context): View = LinearLayout(context).apply {
        id = R.id.layoutNumpadPanel
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP
        }
        addView(createNumpadDigitsRow(context))
        addView(createNumpadPunctuationContainer(context))
        addView(createNumpadBottomBar(context))
    }

    private fun createNumpadDigitsRow(context: Context): View = LinearLayout(context).apply {
        id = R.id.rowNumpadDigits
        orientation = LinearLayout.HORIZONTAL
        weightSum = 10f
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(context, 4)
        }
        listOf(
            R.id.np_key_1 to "1",
            R.id.np_key_2 to "2",
            R.id.np_key_3 to "3",
            R.id.np_key_4 to "4",
            R.id.np_key_5 to "5",
            R.id.np_key_6 to "6",
            R.id.np_key_7 to "7",
            R.id.np_key_8 to "8",
            R.id.np_key_9 to "9",
            R.id.np_key_0 to "0"
        ).forEachIndexed { index, item ->
            val (id, label) = item
            addView(numpadTextKey(context, id, label, marginEndDp = if (index == 9) 0 else 4))
        }
    }

    private fun createNumpadPunctuationContainer(context: Context): View = LinearLayout(context).apply {
        id = R.id.containerPunct
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        addView(
            LinearLayout(context).apply {
                id = R.id.rowPunctWrap
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                addView(
                    createPunctRow(
                        context,
                        R.id.rowPunct1,
                        listOf("，", "。", "、", "！", "？", "：", "；", "“", "”", "@")
                    )
                )
                addView(createSecondPunctRow(context))
            }
        )
    }

    private fun createPunctRow(context: Context, rowId: Int, labels: List<String>): View = LinearLayout(context).apply {
        id = rowId
        orientation = LinearLayout.HORIZONTAL
        weightSum = 10f
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(context, 4)
        }
        labels.forEachIndexed { index, label ->
            addView(numpadTextKey(context, View.NO_ID, label, marginEndDp = if (index == labels.lastIndex) 0 else 4))
        }
    }

    private fun createSecondPunctRow(context: Context): View = LinearLayout(context).apply {
        id = R.id.rowPunct2
        orientation = LinearLayout.HORIZONTAL
        weightSum = 10f
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(context, 4)
        }
        listOf("（", ")", "[", "]", "{", "}", "/", "`").forEachIndexed { index, label ->
            addView(numpadTextKey(context, View.NO_ID, label, marginEndDp = if (index == 7) 4 else 4))
        }
        addView(
            numpadImageButton(
                context,
                R.id.np_btnBackspace,
                R.string.cd_backspace,
                R.drawable.backspace_toggle,
                weight = 2f,
                marginEndDp = 0
            )
        )
    }

    private fun createNumpadBottomBar(context: Context): View = LinearLayout(context).apply {
        id = R.id.rowNumpadBottomBar
        orientation = LinearLayout.HORIZONTAL
        weightSum = 8f
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(context, 4)
        }
        addView(
            numpadImageButton(
                context,
                R.id.np_btnBack,
                R.string.cd_numpad_back,
                R.drawable.arrow_u_up_left_toggle,
                weight = 2f,
                marginEndDp = 4
            )
        )
        addView(
            numpadImageButton(
                context,
                R.id.np_btnPunctToggle,
                R.string.cd_punct_lang_toggle,
                R.drawable.translate,
                weight = 1f,
                marginEndDp = 4
            )
        )
        addView(
            numpadTextKey(context, R.id.np_key_space, context.getString(R.string.cd_space), weight = 3f, marginEndDp = 4).apply {
                contentDescription = context.getString(R.string.cd_space)
            }
        )
        addView(
            numpadImageButton(
                context,
                R.id.np_btnEnter,
                R.string.cd_enter,
                R.drawable.key_return_toggle,
                weight = 2f,
                marginEndDp = 0
            )
        )
    }

    private fun createMicStatusGroup(context: Context): View = LinearLayout(context).apply {
        id = R.id.groupMicStatus
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        translationY = dp(context, 3).toFloat()
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        addView(
            FloatingActionButton(context).apply {
                id = R.id.btnMic
                contentDescription = context.getString(R.string.cd_mic)
                setImageResource(R.drawable.microphone)
                backgroundTintList = ColorStateList.valueOf(
                    UiColors.get(context, UiColorTokens.secondaryContainer)
                )
                imageTintList = ColorStateList.valueOf(
                    UiColors.get(context, UiColorTokens.onSecondaryContainer)
                )
                elevation = 0f
                compatElevation = 0f
                stateListAnimator = null
                customSize = dp(context, 72)
                setMaxImageSize(dp(context, 40))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
        )
        addView(
            TextView(context).apply {
                id = R.id.txtStatus
                text = context.getString(R.string.status_idle)
                gravity = Gravity.CENTER
                maxLines = 3
                ellipsize = TextUtils.TruncateAt.END
                setTextColor(UiColors.get(context, UiColorTokens.panelFgVariant))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = Typeface.create(typeface, Typeface.BOLD)
                includeFontPadding = false
                visibility = View.GONE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = dp(context, 90)
                    marginEnd = dp(context, 90)
                    topMargin = dp(context, 16)
                }
            }
        )
    }

    private fun imageButton(
        context: Context,
        id: Int,
        contentDescriptionRes: Int,
        imageRes: Int? = null
    ): ImageButton = ImageButton(context).apply {
        this.id = id
        contentDescription = context.getString(contentDescriptionRes)
        layoutParams = ConstraintLayout.LayoutParams(dp(context, 40), dp(context, 40))
        background = ContextCompat.getDrawable(context, R.drawable.bg_key_button)
        foreground = selectableBorderless(context)
        clipToOutline = true
        setPadding(dp(context, 6), dp(context, 6), dp(context, 6), dp(context, 6))
        imageTintList = ColorStateList.valueOf(UiColors.get(context, UiColorTokens.kbdKeyFg))
        scaleType = ImageView.ScaleType.CENTER
        stateListAnimator = AnimatorInflater.loadStateListAnimator(
            context,
            R.animator.key_button_state_list
        )
        imageRes?.let(::setImageResource)
    }

    private fun numpadImageButton(
        context: Context,
        id: Int,
        contentDescriptionRes: Int,
        imageRes: Int,
        weight: Float,
        marginEndDp: Int
    ): ImageButton = ImageButton(context).apply {
        this.id = id
        tag = "key40"
        contentDescription = context.getString(contentDescriptionRes)
        background = ContextCompat.getDrawable(context, R.drawable.bg_key_rect_button)
        clipToOutline = true
        setPadding(dp(context, 6), dp(context, 6), dp(context, 6), dp(context, 6))
        imageTintList = ColorStateList.valueOf(UiColors.get(context, UiColorTokens.kbdKeyFg))
        scaleType = ImageView.ScaleType.CENTER
        stateListAnimator = AnimatorInflater.loadStateListAnimator(
            context,
            R.animator.key_button_state_list
        )
        setImageResource(imageRes)
        layoutParams = LinearLayout.LayoutParams(0, dp(context, 40)).apply {
            this.weight = weight
            marginEnd = dp(context, marginEndDp)
        }
    }

    private fun numpadTextKey(
        context: Context,
        id: Int,
        label: String,
        weight: Float = 1f,
        marginEndDp: Int
    ): TextView = TextView(context).apply {
        if (id != View.NO_ID) this.id = id
        tag = "key40"
        text = label
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true
        background = ContextCompat.getDrawable(context, R.drawable.bg_key_rect_button)
        clipToOutline = true
        setTextColor(UiColors.get(context, UiColorTokens.kbdKeyFg))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        includeFontPadding = false
        setPadding(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4))
        layoutParams = LinearLayout.LayoutParams(0, dp(context, 40)).apply {
            this.weight = weight
            marginEnd = dp(context, marginEndDp)
        }
    }

    private fun punctButton(context: Context, id: Int, contentDescriptionRes: Int): PunctKeyView = PunctKeyView(context).apply {
        this.id = id
        contentDescription = context.getString(contentDescriptionRes)
        layoutParams = ConstraintLayout.LayoutParams(dp(context, 40), dp(context, 40))
        background = ContextCompat.getDrawable(context, R.drawable.bg_key_button)
        foreground = selectableBorderless(context)
        clipToOutline = true
        isClickable = true
        isFocusable = true
        stateListAnimator = AnimatorInflater.loadStateListAnimator(
            context,
            R.animator.key_button_state_list
        )
    }

    private fun keyFrame(context: Context, id: Int): FrameLayout = FrameLayout(context).apply {
        this.id = id
        background = ContextCompat.getDrawable(context, R.drawable.bg_key_rect_button)
        clipToOutline = true
        stateListAnimator = AnimatorInflater.loadStateListAnimator(
            context,
            R.animator.key_button_state_list
        )
    }

    private fun keyButton(context: Context, id: Int, contentDescriptionRes: Int): Button = Button(context).apply {
        this.id = id
        contentDescription = context.getString(contentDescriptionRes)
        background = ContextCompat.getDrawable(context, R.drawable.bg_key_rect_button)
        clipToOutline = true
        stateListAnimator = AnimatorInflater.loadStateListAnimator(
            context,
            R.animator.key_button_state_list
        )
        setTextColor(UiColors.get(context, UiColorTokens.kbdKeyFg))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        gravity = Gravity.CENTER
        includeFontPadding = false
        isAllCaps = false
        minHeight = 0
        minWidth = 0
        minimumHeight = 0
        minimumWidth = 0
    }

    private fun gestureButton(context: Context, id: Int, textRes: Int): TextView = TextView(context).apply {
        this.id = id
        text = context.getString(textRes)
        background = ContextCompat.getDrawable(context, R.drawable.bg_key_rect_button)
        foreground = selectableBorderless(context)
        clipToOutline = true
        isClickable = true
        isFocusable = true
        gravity = Gravity.CENTER
        setTextColor(UiColors.get(context, UiColorTokens.kbdKeyFg))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        layoutParams = ConstraintLayout.LayoutParams(dp(context, 86), dp(context, 86))
    }

    private fun statusText(context: Context, id: Int): TextView = TextView(context).apply {
        this.id = id
        gravity = Gravity.CENTER
        setTextColor(UiColors.get(context, UiColorTokens.panelFg))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        isSingleLine = true
        ellipsize = TextUtils.TruncateAt.MARQUEE
        marqueeRepeatLimit = -1
        isFocusable = true
        isFocusableInTouchMode = true
        includeFontPadding = false
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
    }

    private fun View.withConstraints(
        block: ConstraintLayout.LayoutParams.() -> Unit
    ): View = apply {
        val existing = layoutParams as? ConstraintLayout.LayoutParams
        layoutParams = (existing ?: ConstraintLayout.LayoutParams(dp(context, 40), dp(context, 40)))
            .apply(block)
    }

    private fun selectableBorderless(context: Context): android.graphics.drawable.Drawable? {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
        return ContextCompat.getDrawable(context, outValue.resourceId)
    }

    private fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density + 0.5f).toInt()

    private fun View.applyRectKeyTheme(theme: com.brycewg.asrkb.ui.BibiViewTheme) {
        background = BibiViewThemes.roundedRipple(
            context,
            theme.keyBackground,
            theme.ripple,
            theme.rectKeyRadiusDp,
            insetDp = theme.keyInsetDp
        )
        when (this) {
            is TextView -> setTextColor(theme.keyContent)
            is ImageButton -> imageTintList = ColorStateList.valueOf(theme.keyContent)
        }
    }

    private fun applyTaggedKeys(view: View, theme: com.brycewg.asrkb.ui.BibiViewTheme) {
        if (view.tag == "key40") {
            view.applyRectKeyTheme(theme)
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyTaggedKeys(view.getChildAt(i), theme)
            }
        }
    }

    private val iconKeyIds = intArrayOf(
        R.id.btnExt1,
        R.id.btnExt2,
        R.id.btnExt3,
        R.id.btnExt4,
        R.id.btnHide,
        R.id.btnPostproc,
        R.id.btnBackspace,
        R.id.btnPromptPicker,
        R.id.btnSettings,
        R.id.btnImeSwitcher,
        R.id.btnEnter,
        R.id.btnAiEdit,
        R.id.btnPunct1,
        R.id.btnPunct4,
        R.id.btnAiPanelBack,
        R.id.btnAiPanelApplyPreset,
        R.id.btnAiPanelCursorLeft,
        R.id.btnAiPanelCursorRight,
        R.id.btnAiPanelMoveStart,
        R.id.btnAiPanelMoveEnd,
        R.id.btnAiPanelSelect,
        R.id.btnAiPanelSelectAll,
        R.id.btnAiPanelCopy,
        R.id.btnAiPanelPaste,
        R.id.btnAiPanelUndo,
        R.id.btnAiPanelNumpad,
        R.id.clip_btnBack,
        R.id.clip_btnDelete
    )

    private val rectKeyIds = intArrayOf(
        R.id.btnExtCenter1,
        R.id.btnExtCenter2,
        R.id.aiEditInfoBar,
        R.id.btnAiPanelSpace,
        R.id.btnGestureCancel,
        R.id.btnGestureSend
    )

    private val statusTextIds = intArrayOf(
        R.id.txtStatusText,
        R.id.txtAiEditInfo,
        R.id.txtStatus,
        R.id.clip_txtCount
    )
}
