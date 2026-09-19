import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/services/capture/capture_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class ScreenshotCaptureSettingTile extends StatefulWidget {
  const ScreenshotCaptureSettingTile({super.key});

  @override
  State<ScreenshotCaptureSettingTile> createState() =>
      _ScreenshotCaptureSettingTileState();
}

class _ScreenshotCaptureSettingTileState
    extends State<ScreenshotCaptureSettingTile> {
  final CaptureService _capture = CaptureService.instance;

  @override
  void initState() {
    super.initState();
    _capture.watchEnabled.addListener(_refresh);
    _capture.autoAvailable.addListener(_refresh);
    _capture.autoEnabled.addListener(_refresh);
    _capture.autoJob.addListener(_refresh);
    _capture.busy.addListener(_refresh);
    _capture.initialize().catchError((_) {});
  }

  @override
  void dispose() {
    _capture.watchEnabled.removeListener(_refresh);
    _capture.autoAvailable.removeListener(_refresh);
    _capture.autoEnabled.removeListener(_refresh);
    _capture.autoJob.removeListener(_refresh);
    _capture.busy.removeListener(_refresh);
    super.dispose();
  }

  void _refresh() {
    if (mounted) setState(() {});
  }

  Future<void> _showConfiguration() async {
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text('截图识别与生成'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SwitchListTile.adaptive(
                contentPadding: EdgeInsets.zero,
                title: const Text('监听新截图'),
                subtitle: const Text(
                  '开启时会依次申请照片、通知和“显示在其他应用上层”。缺少任一权限都不会显示为已开启。',
                ),
                value: _capture.watchEnabled.value,
                onChanged: _capture.busy.value
                    ? null
                    : (value) async {
                        try {
                          final changed = await _capture.setWatchEnabled(value);
                          if (!changed && context.mounted) {
                            _message(
                              context,
                              _capture.lastWatchMessage ??
                                  '所需系统权限未全部获得，截图监听没有开启',
                            );
                          }
                        } on PlatformException catch (error) {
                          if (context.mounted) {
                            _message(context, error.message ?? '无法更改截图监听状态');
                          }
                        } finally {
                          if (context.mounted) {
                            setDialogState(() {});
                          }
                        }
                      },
              ),
              SwitchListTile.adaptive(
                contentPadding: EdgeInsets.zero,
                title: const Text('自动生成新截图'),
                subtitle: Text(
                  _capture.autoAvailable.value
                      ? '单独授权后，仅自动处理此后发现的新截图。'
                      : (_capture.lastAutoMessage ?? '当前版本暂不支持后台自动生成。'),
                ),
                value: _capture.autoEnabled.value,
                onChanged: _capture.busy.value ||
                        !_capture.watchEnabled.value ||
                        !_capture.autoAvailable.value
                    ? null
                    : (value) async {
                        if (value) {
                          final consent = await showDialog<bool>(
                                context: context,
                                barrierDismissible: false,
                                builder: (consentContext) => AlertDialog(
                                  title: const Text('允许自动上传并生成？'),
                                  content: const Text(
                                    '开启后，应用会对授权之后新发现的系统截图先在设备上识别文字，再自动上传图片并生成备忘录。\n\n历史待处理图片、从其他应用分享的图片和手动选择的图片仍会逐张询问。失败任务不会自动重复上传，只能由你明确重试。',
                                  ),
                                  actions: [
                                    TextButton(
                                      onPressed: () =>
                                          Navigator.of(consentContext)
                                              .pop(false),
                                      child: const Text('取消'),
                                    ),
                                    FilledButton(
                                      onPressed: () =>
                                          Navigator.of(consentContext)
                                              .pop(true),
                                      child: const Text('允许自动生成'),
                                    ),
                                  ],
                                ),
                              ) ??
                              false;
                          if (!consent) return;
                        }
                        try {
                          final changed =
                              await _capture.setAutoGenerationEnabled(value);
                          if (!changed && context.mounted) {
                            _message(context,
                                _capture.lastAutoMessage ?? '无法更改自动生成设置');
                          }
                        } on PlatformException catch (error) {
                          if (context.mounted) {
                            _message(context, error.message ?? '无法更改自动生成设置');
                          }
                        } finally {
                          if (context.mounted) setDialogState(() {});
                        }
                      },
              ),
              if (_capture.autoJob.value case final job?) ...[
                const SizedBox(height: 4),
                Text(
                  _jobDescription(job),
                  style: const TextStyle(fontSize: 13, color: Colors.black54),
                ),
                if (job['state'] == 'failed' || job['state'] == 'paused')
                  Align(
                    alignment: Alignment.centerLeft,
                    child: TextButton(
                      onPressed: () async {
                        final retried = await _capture.retryAutoJob();
                        if (!retried && context.mounted) {
                          _message(context, '任务暂时无法重试，请检查监听、权限和设备身份');
                        }
                        if (context.mounted) setDialogState(() {});
                      },
                      child: const Text('重试保留的任务'),
                    ),
                  ),
              ],
              const SizedBox(height: 8),
              const Text(
                '系统只检查新加入媒体库且名称或相册标识为截图的图片，无法判断截图来自哪个应用。未开启自动生成时，每张图片都要先确认；开启后也只自动处理授权之后发现的新截图。最多保留 20 张待处理图片。关闭截图监听会同时关闭自动生成。部分手机若限制应用后台运行，常驻通知可能消失，此时需在系统设置中允许后台运行。',
                style: TextStyle(fontSize: 13, color: Colors.black54),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () async {
                Navigator.of(dialogContext).pop();
                try {
                  await _capture.pickImage();
                } on PlatformException catch (error) {
                  if (mounted) {
                    _message(this.context, error.message ?? '无法选择图片');
                  }
                }
              },
              child: const Text('手动选择图片'),
            ),
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(),
              child: const Text('完成'),
            ),
          ],
        ),
      ),
    );
  }

  void _message(BuildContext context, String message) {
    ScaffoldMessenger.maybeOf(context)?.showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  String _jobDescription(Map<Object?, Object?> job) {
    final message = job['errorMessage'];
    return switch (job['state']) {
      'ocr' => '当前任务：正在设备本地识别，尚未上传。',
      'uploading' => '当前任务：正在上传图片并生成备忘录。',
      'failed' => '当前任务失败：${message is String ? message : '图片已保留'}',
      'paused' => '当前任务已暂停：${message is String ? message : '图片已保留'}',
      _ => '当前任务：等待处理。',
    };
  }

  @override
  Widget build(BuildContext context) {
    final enabled = _capture.watchEnabled.value;
    return GestureDetector(
      onTap: _showConfiguration,
      child: Container(
        height: 52.w,
        width: double.infinity,
        padding: EdgeInsets.symmetric(horizontal: 16.w),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(8.w),
        ),
        child: Row(
          children: [
            Icon(Icons.screenshot_monitor_outlined, size: 24.w),
            12.horizontalSpace,
            Text(
              '截图识别与生成',
              style: TextStyle(fontSize: 16.sp, fontWeight: FontWeight.w600),
            ),
            const Spacer(),
            Text(
              enabled ? '已开启' : '未开启',
              style: TextStyle(
                fontSize: 16.sp,
                color: enabled ? MyColors.colorBlue : MyColors.colorRed,
                fontWeight: FontWeight.w400,
              ),
            ),
            12.horizontalSpace,
            Icon(Icons.arrow_forward_ios, size: 16.w, color: Colors.black),
          ],
        ),
      ),
    );
  }
}
