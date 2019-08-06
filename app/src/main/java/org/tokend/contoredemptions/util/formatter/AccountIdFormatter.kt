package org.tokend.contoredemptions.util.formatter

class AccountIdFormatter {
    fun formatShort(accountId: String): String {
        return "${accountId.substring(0..3)}…" +
                accountId.substring(accountId.length - 4 until accountId.length)
    }
}