package de.dertyp7214.youtubemusicremote.components

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.dertyp7214.youtubemusicremote.core.blur
import java.lang.Float.max
import java.lang.Float.min
import kotlin.math.roundToInt

open class BaseBottomSheet : BottomSheetDialogFragment() {
    val isShowing
        get() = dialog?.isShowing ?: false

    protected val state: MutableLiveData<Int> = MutableLiveData()

    protected open var blurFunction: (Boolean) -> Unit = {}

    fun showWithBlur(appCompatActivity: AppCompatActivity, blurContent: View, blurView: View) {
        if (!isShowing && instance == null) {
            blurFunction = { clear: Boolean ->
                if (!clear) blur(appCompatActivity, blurContent) {
                    blurView.foreground = it
                } else blurView.foreground = null
            }.also { it(false) }

            val oldFragment = appCompatActivity.supportFragmentManager.findFragmentByTag(
                TAG
            )

            if (!isAdded && oldFragment == null) try {
                show(
                    appCompatActivity.supportFragmentManager,
                    TAG
                )
                instance = this
            } catch (_: Exception) {
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        blurFunction(true)
        instance = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).let {
            val dialog = it as BottomSheetDialog
            val behavior: BottomSheetBehavior<*> = dialog.behavior
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) dismiss()
                    state.postValue(newState)
                }
            })
            val height = requireActivity().window.decorView.height.toFloat()
            val width = requireActivity().window.decorView.width.toFloat()
            val peekHeight = max(height, width) - min(height, width) * 9 / 16
            behavior.setPeekHeight(peekHeight.roundToInt(), true)
            dialog
        }
    }

    companion object {
        const val TAG = "BaseBottomSheet"

        private var instance: BaseBottomSheet? = null
    }
}