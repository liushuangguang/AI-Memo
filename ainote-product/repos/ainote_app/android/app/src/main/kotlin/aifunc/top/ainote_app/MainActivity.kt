package aifunc.top.ainote_app

import android.content.Intent
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

class MainActivity : FlutterActivity() {
    private var captureBridge: CaptureBridge? = null
    private var voiceBridge: VoiceBridge? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        captureBridge = CaptureBridge(this, flutterEngine.dartExecutor.binaryMessenger).also {
            it.attach()
            it.handleIntent(intent)
        }
        voiceBridge = VoiceBridge.register(this, flutterEngine.dartExecutor.binaryMessenger)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureBridge?.handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        ScreenshotWatchService.onHostActivityResumed()
        captureBridge?.onActivityResumed()
    }

    override fun onPause() {
        ScreenshotWatchService.onHostActivityPaused()
        super.onPause()
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (captureBridge?.onActivityResult(requestCode, resultCode, data) == true) return
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        if (voiceBridge?.onRequestPermissionsResult(requestCode, grantResults) == true) return
        if (captureBridge?.onRequestPermissionsResult(requestCode) == true) return
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        ScreenshotWatchService.onHostActivityPaused()
        captureBridge?.detach()
        captureBridge = null
        voiceBridge?.destroy()
        voiceBridge = null
        super.onDestroy()
    }
}
