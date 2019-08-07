package org.tokend.contoredemptions.features.qr.model

class NoCameraPermissionException
    : IllegalStateException("Camera permission is required to perform this action")