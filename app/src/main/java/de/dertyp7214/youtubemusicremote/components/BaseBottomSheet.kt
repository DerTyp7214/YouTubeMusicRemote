package de.dertyp7214.youtubemusicremote.components

import android.content.DialogInterface
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.dertyp7214.youtubemusicremote.core.blur

open class BaseBottomSheet : BottomSheetDialogFragment() {
    val isShowing
        get() = dialog?.isShowing ?: false

    internal open var blurFunction: (Boolean) -> Unit = {}

    fun showWithBlur(appCompatActivity: AppCompatActivity, blurContent: View, blurView: View) {
        if (!isShowing && instance == null) {
            blurFunction = { clear: Boolean ->
                blur(appCompatActivity, blurContent) {
                    blurView.foreground = if (clear) null else it
                }
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

    companion object {
        const val TAG = "BaseBottomSheet"

        private var instance: BaseBottomSheet? = null
    }
}