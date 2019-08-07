package org.tokend.contoredemptions.view.util

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import androidx.annotation.StringRes
import org.tokend.contoredemptions.R

object ProgressDialogFactory {

    fun getDialog(context: Context?,
                  @StringRes
                  messageRes: Int = R.string.processing_progress,
                  cancelListener: ((DialogInterface) -> Unit)? = null
    ): ProgressDialog {
        val dialog = ProgressDialog(context)
        dialog.isIndeterminate = true
        dialog.setMessage(context?.getString(messageRes))
        if (cancelListener != null) {
            dialog.setCancelable(true)
            dialog.setOnCancelListener(cancelListener)
        } else {
            dialog.setCancelable(false)
        }
        return dialog
    }
}