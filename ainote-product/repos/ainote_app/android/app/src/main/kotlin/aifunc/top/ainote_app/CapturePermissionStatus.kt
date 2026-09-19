package aifunc.top.ainote_app

internal data class CapturePermissionStatus(
    val imageGranted: Boolean,
    val notificationsGranted: Boolean,
    val overlayGranted: Boolean,
) {
    val allGranted: Boolean
        get() = imageGranted && notificationsGranted && overlayGranted

    fun toMap(): Map<String, Boolean> = mapOf(
        "imageGranted" to imageGranted,
        "notificationsGranted" to notificationsGranted,
        "overlayGranted" to overlayGranted,
        "allGranted" to allGranted,
    )

    fun denialMessage(): String = when {
        !imageGranted && !notificationsGranted && !overlayGranted ->
            "需要允许照片、通知和显示在其他应用上层，截图识别才会开启"
        !imageGranted -> "需要允许访问照片，才能发现新截图"
        !notificationsGranted -> "需要允许通知，才能持续显示截图监听状态"
        !overlayGranted -> "需要允许显示在其他应用上层，才能在截图后显示确认浮层"
        else -> "截图识别所需权限已获得"
    }
}
