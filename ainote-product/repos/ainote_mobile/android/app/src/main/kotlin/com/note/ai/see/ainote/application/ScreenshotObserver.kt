package com.note.ai.see.ainote.application

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import java.io.File
import java.util.Locale

class ScreenshotObserver : ContentObserver(null), FlutterPlugin, MethodCallHandler {
    private var channel: MethodChannel? = null
    private var context: Context? = null

    //媒体内容监听器
    private var mContentResolver: ContentResolver? = null

    //文件监听器
    private var fileObserver: FileObserver? = null

    //最后监听到的文件路径，用于去除重复事件
    private var mLastObservePath: String? = null
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if (call.method == "startObserving") {
            startObserving()
            result.success(null)
        } else if (call.method == "stopObserving") {
            onDestroy()
            result.success(null)
        } else {
            result.notImplemented()
        }
    }

    override fun onAttachedToEngine(binding: FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "screenshot_observer")
        channel!!.setMethodCallHandler(this)
        context = binding.applicationContext
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        onDestroy()
        channel = null
    }

    //系统截屏文件目录
    object ScreenShotPath {
        val NORMAL = (Environment.getExternalStorageDirectory()
            .toString() + File.separator + Environment.DIRECTORY_PICTURES
                + File.separator + "Screenshots" + File.separator)
        val XIAOMI = (Environment.getExternalStorageDirectory()
            .toString() + File.separator + Environment.DIRECTORY_DCIM
                + File.separator + "Screenshots" + File.separator)
        val VIVO = Environment.getExternalStorageDirectory()
            .toString() + File.separator + "截屏" + File.separator
    }

    private fun startObserving() {
        //判断版本是否小于6.0
        if (Build.VERSION.SDK_INT < 23) {
            //判断厂商，设置监听目录
            val screenshotPath: String =
                if (Build.MANUFACTURER.equals(MANUFACTURER_OF_HARDWARE_XIAOMI, ignoreCase = true)) {
                    ScreenShotPath.XIAOMI
                } else if (Build.MANUFACTURER.equals(
                        MANUFACTURER_OF_HARDWARE_VIVO,
                        ignoreCase = true
                    )
                ) {
                    ScreenShotPath.VIVO
                } else {
                    ScreenShotPath.NORMAL
                }
            //创建文件监听器
            fileObserver = object : FileObserver(screenshotPath, CREATE) {
                override fun onEvent(event: Int, path: String?) {
                    Log.d(TAG, "进入onEvent方法")
                    //过滤错误参数及事件
                    if (TextUtils.isEmpty(path) || event != CREATE) {
                        return
                    }
                    //过滤重复事件以及新MIUI截屏临时文件
                    if (path.equals(
                            mLastObservePath,
                            ignoreCase = true
                        ) || path!!.contains("temp")
                    ) {
                        return
                    }
                    //通知回调
                    mLastObservePath = path
                    Handler(Looper.getMainLooper()).post { // 在这里调用 channel.invokeMethod()
                        channel!!.invokeMethod("onScreenshot", screenshotPath + path)
                    }
                }
            }
        } else {
            mContentResolver = context!!.contentResolver
        }
        registerToSystem()
    }

    /**
     * 清除资源
     */
    private fun onDestroy() {
        unregisterToSystem()
        fileObserver = null
        mContentResolver = null
    }

    /**
     * 开始监听
     */
    private fun registerToSystem() {
        //判断版本是否小于6.0
        if (Build.VERSION.SDK_INT < 23) {
            fileObserver!!.startWatching()
        } else {
            mContentResolver!!.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                this
            )
        }
    }

    /**
     * 关闭监听
     */
    private fun unregisterToSystem() {
        //判断版本是否小于6.0
        if (Build.VERSION.SDK_INT < 23) {
            fileObserver!!.stopWatching()
        } else {
            //不需要监听的时候，一定要把原来的ContentObserver注销掉。
            mContentResolver!!.unregisterContentObserver(this)
        }
    }

    /**
     * 媒体内容监听器回调
     *
     * @param selfChange
     */
    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        //从API16开始，才有两个参数的onChange方法，所以这里要主动调用下面的onChange方法。
        onChange(selfChange, null)
    }

    /**
     * 媒体内容监听器回调
     *
     * @param selfChange
     * @param uri
     */
    override fun onChange(selfChange: Boolean, uri: Uri?) {
        Log.d(TAG, "进入onChange方法")
        var cursor: Cursor? = null
        if (uri == null) { //API16以下版本
            Log.d(TAG, "进入onChange方法-uri=null")
            try {
                cursor = mContentResolver!!.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    PROJECTION,
                    null,
                    null,
                    SORT_ORDER
                )
                if (cursor != null && cursor.count > 0) {
                    cursor.moveToFirst()
                    //完整路径
                    val dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                    if (dataIndex < 0) {
                        return
                    }
                    val path = cursor.getString(dataIndex)
                    //添加图片的时间，单位秒
                    val addedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                    if (addedIndex < 0) {
                        return
                    }
                    val addTime = cursor.getLong(addedIndex)
                    //加个过滤条件必须是3S内的图片，且路径中包含截图字样“screenshot”
                    if (matchAddTime(addTime) && matchPath(path) /*&& matchSize(path)*/) {
                        if (path != null) {
                            mLastObservePath =
                                if (path.equals(mLastObservePath, ignoreCase = true)) {
                                    return
                                } else {
                                    path
                                }
                        }
                        //这就是系统截屏的图片了，这里测试发现需要等待几百MS，才能加载到图片。因此具体实现时，最好在独立线程，每隔100MS尝试加载一次，做好超时处理。
                        Log.d(TAG, "onChange方法-uri=null,path = $path")
                        mLastObservePath = path
                        Handler(Looper.getMainLooper()).post { // 在这里调用 channel.invokeMethod()
                            channel!!.invokeMethod("onScreenshot", path)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (cursor != null) {
                    try {
                        cursor.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else { //API16及以上版本
            Log.d(TAG, "进入onChange方法-uri=$uri")
            try {
                if (uri.toString().startsWith(EXTERNAL_CONTENT_URI_MATCHER)) {
                    cursor = mContentResolver!!.query(uri, PROJECTION, null, null, SORT_ORDER)
                    if (cursor != null && cursor.count > 0) {
                        cursor.moveToFirst()
                        //完整路径
                        val dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                        if (dataIndex < 0) {
                            return
                        }
                        val path = cursor.getString(dataIndex)
                        //添加图片的时间，单位秒
                        val addedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                        if (addedIndex < 0) {
                            return
                        }
                        val addTime = cursor.getLong(addedIndex)
                        if (matchAddTime(addTime) && matchPath(path) /*&& matchSize(path)*/) {
                            if (path != null) {
                                mLastObservePath =
                                    if (path.equals(mLastObservePath, ignoreCase = true)) {
                                        return
                                    } else {
                                        path
                                    }
                            }
                            //这就是系统截屏的图片了
                            Log.d(TAG, "onChange方法-uri!=null,path = $path")
                            mLastObservePath = path
                            Handler(Looper.getMainLooper()).post {
                                channel!!.invokeMethod(
                                    "onScreenshot",
                                    path
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (cursor != null) {
                    try {
                        cursor.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    /**
     * 添加时间与当前时间不超过1.5s,大部分时候不超过1s。
     *
     * @param addTime 图片添加时间，单位:秒
     */
    private fun matchAddTime(addTime: Long): Boolean {
        return System.currentTimeMillis() - addTime * 1000 < TIME_MATCH_MILLISECOND
    }

    /**
     * 已调查的手机截屏图片的路径中带有screenshot
     */
    private fun matchPath(filePath: String?): Boolean {
        val lower = filePath!!.lowercase(Locale.getDefault())
        return lower.contains(DISPLAY_NAME_EN) || lower.contains(DISPLAY_NAME_CN)
    }

    companion object {
        private const val TAG = "ScreenshotObserver"

        //URI匹配
        private val EXTERNAL_CONTENT_URI_MATCHER =
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString()

        //查询的表字段
        private val PROJECTION =
            arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_ADDED)

        //根据时间降序排序
        private const val SORT_ORDER = MediaStore.Images.Media.DATE_ADDED + " DESC"

        //时间判断间隔标准
        private const val TIME_MATCH_MILLISECOND = 1500L

        //英文名称判断标准
        private const val DISPLAY_NAME_EN = "screenshot"

        //中文名称判断标准
        private const val DISPLAY_NAME_CN = "截屏"
        const val MANUFACTURER_OF_HARDWARE_XIAOMI = "xiaomi"
        const val MANUFACTURER_OF_HARDWARE_VIVO = "vivo"
    }
}