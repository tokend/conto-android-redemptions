package org.tokend.contoredemptions.util.errorhandler

interface ErrorLogger {
    fun log(error: Throwable)
}