package org.tokend.contoredemptions.util.errorhandler

class DefaultErrorLogger : ErrorLogger {

    override fun log(error: Throwable) {
        error.printStackTrace()
    }
}