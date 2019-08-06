package org.tokend.contoredemptions.view.util

import android.content.Context
import org.tokend.contoredemptions.R
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Holds localized name getters for enums
 */
class LocalizedName(private val context: Context) {
    fun forMonth(numberFromZero: Int): String {
        return context.resources.getStringArray(R.array.months)[numberFromZero]
    }
}