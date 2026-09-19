import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../controllers/voice_discuss_controller.dart';
import '../models/voice_discussion_models.dart';

class VoiceDiscussView extends GetView<VoiceDiscussController> {
  const VoiceDiscussView({super.key});

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) _close();
      },
      child: Scaffold(
        backgroundColor: const Color(0xFFF6F7FB),
        appBar: AppBar(
          title: const Text('语音讨论'),
          centerTitle: true,
          leading: IconButton(
            tooltip: '关闭',
            onPressed: _close,
            icon: const Icon(Icons.close_rounded),
          ),
          actions: [
            Obx(() => TextButton(
                  onPressed: controller.canSummarize
                      ? () => _showSummary(context)
                      : null,
                  child: const Text('总结'),
                )),
          ],
        ),
        body: SafeArea(
          child: Column(
            children: [
              _NoteContextHeader(controller: controller),
              Obx(() {
                final error = controller.errorText.value;
                if (error == null) return const SizedBox.shrink();
                return Container(
                  width: double.infinity,
                  color: const Color(0xFFFFF1E8),
                  padding:
                      const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                  child: Text(error,
                      style: const TextStyle(color: Color(0xFF9B3D12))),
                );
              }),
              Expanded(
                child: Obx(() {
                  if (controller.messages.isEmpty) {
                    return const _EmptyConversation();
                  }
                  return ListView.builder(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
                    itemCount: controller.messages.length,
                    itemBuilder: (context, index) => _MessageBubble(
                      message: controller.messages[index],
                      index: index,
                      controller: controller,
                    ),
                  );
                }),
              ),
              _Composer(controller: controller),
            ],
          ),
        ),
      ),
    );
  }

  void _close() {
    Get.back(result: {
      'noteUpdated': controller.summarySaved.value,
      'note': controller.savedNote.value,
    });
  }

  Future<void> _showSummary(BuildContext context) async {
    final generated = await controller.generateSummaryPreview();
    if (!generated || !context.mounted) return;
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (sheetContext) => Padding(
        padding: EdgeInsets.only(
          left: 20,
          right: 20,
          top: 18,
          bottom: MediaQuery.viewInsetsOf(sheetContext).bottom + 20,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text('总结预览',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700)),
            const SizedBox(height: 6),
            const Text('确认保存前不会修改当前笔记。你也可以先编辑总结。',
                style: TextStyle(color: Color(0xFF667085))),
            const SizedBox(height: 16),
            TextField(
              controller: controller.summaryController,
              minLines: 6,
              maxLines: 12,
              decoration: const InputDecoration(
                border: OutlineInputBorder(),
                labelText: '总结内容',
                alignLabelWithHint: true,
              ),
            ),
            const SizedBox(height: 16),
            Obx(() => FilledButton.icon(
                  onPressed: controller.isSummarySaving.value
                      ? null
                      : () async {
                          final saved = await controller.saveSummary();
                          if (saved && sheetContext.mounted) {
                            Navigator.of(sheetContext).pop();
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(content: Text('总结已写入当前笔记')),
                            );
                          }
                        },
                  icon: controller.isSummarySaving.value
                      ? const SizedBox.square(
                          dimension: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.save_outlined),
                  label: const Text('确认保存到当前笔记'),
                )),
            TextButton(
              onPressed: () => Navigator.of(sheetContext).pop(),
              child: const Text('暂不保存'),
            ),
          ],
        ),
      ),
    );
  }
}

class _NoteContextHeader extends StatelessWidget {
  const _NoteContextHeader({required this.controller});
  final VoiceDiscussController controller;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(16, 10, 16, 10),
      color: Colors.white,
      child: Row(
        children: [
          const Icon(Icons.description_outlined, size: 18),
          const SizedBox(width: 8),
          Expanded(
            child: Text('正在结合「${controller.noteTitle}」讨论',
                maxLines: 1, overflow: TextOverflow.ellipsis),
          ),
        ],
      ),
    );
  }
}

class _EmptyConversation extends StatelessWidget {
  const _EmptyConversation();

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: Padding(
        padding: EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.graphic_eq_rounded, size: 52, color: Color(0xFF6475E8)),
            SizedBox(height: 14),
            Text('从当前笔记继续聊',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700)),
            SizedBox(height: 8),
            Text('点击麦克风说话，识别结果会先出现在输入框中；确认或编辑后再发送。',
                textAlign: TextAlign.center,
                style: TextStyle(color: Color(0xFF667085), height: 1.5)),
          ],
        ),
      ),
    );
  }
}

class _MessageBubble extends StatelessWidget {
  const _MessageBubble({
    required this.message,
    required this.index,
    required this.controller,
  });

  final VoiceDiscussionMessage message;
  final int index;
  final VoiceDiscussController controller;

  @override
  Widget build(BuildContext context) {
    final isUser = message.role == VoiceDiscussionRole.user;
    return Align(
      alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        constraints: const BoxConstraints(maxWidth: 330),
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.fromLTRB(14, 11, 10, 8),
        decoration: BoxDecoration(
          color: isUser ? const Color(0xFF5869E8) : Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: isUser ? null : Border.all(color: const Color(0xFFE5E7EB)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(message.content,
                style: TextStyle(
                  color: isUser ? Colors.white : const Color(0xFF1D2939),
                  height: 1.45,
                )),
            if (message.delivery == VoiceMessageDelivery.pending)
              const Padding(
                padding: EdgeInsets.only(top: 8),
                child: SizedBox.square(
                  dimension: 14,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
              ),
            if (message.canRetry)
              TextButton.icon(
                style: TextButton.styleFrom(
                  foregroundColor: isUser ? Colors.white : null,
                  padding: EdgeInsets.zero,
                ),
                onPressed: () => controller.retryMessage(index),
                icon: const Icon(Icons.refresh_rounded, size: 18),
                label: Text(message.delivery == VoiceMessageDelivery.cancelled
                    ? '已取消，重试'
                    : '发送失败，重试'),
              ),
            if (!isUser)
              Obx(() => TextButton.icon(
                    style: TextButton.styleFrom(padding: EdgeInsets.zero),
                    onPressed: controller.isSpeaking.value
                        ? controller.stopSpeaking
                        : () => controller.speakMessage(message.content),
                    icon: Icon(controller.isSpeaking.value
                        ? Icons.stop_circle_outlined
                        : Icons.volume_up_outlined),
                    label: Text(controller.isSpeaking.value ? '停止朗读' : '朗读'),
                  )),
          ],
        ),
      ),
    );
  }
}

class _Composer extends StatelessWidget {
  const _Composer({required this.controller});
  final VoiceDiscussController controller;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(12, 10, 12, 12),
      decoration: const BoxDecoration(
        color: Colors.white,
        border: Border(top: BorderSide(color: Color(0xFFE5E7EB))),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TextField(
            controller: controller.draftController,
            minLines: 1,
            maxLines: 4,
            textInputAction: TextInputAction.newline,
            decoration: const InputDecoration(
              hintText: '识别文字会出现在这里，可编辑后发送',
              border: OutlineInputBorder(),
              isDense: true,
            ),
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Obx(() => IconButton.filledTonal(
                    tooltip: controller.isListening.value ? '取消识别' : '开始识别',
                    onPressed: controller.isSending.value
                        ? null
                        : controller.isListening.value
                            ? controller.cancelListening
                            : controller.startListening,
                    icon: Icon(controller.isListening.value
                        ? Icons.mic_off_outlined
                        : Icons.mic_none_rounded),
                  )),
              const SizedBox(width: 8),
              const Expanded(
                child: Text('语音不可用时，可直接输入文字继续讨论',
                    style: TextStyle(fontSize: 12, color: Color(0xFF667085))),
              ),
              Obx(() => FilledButton.icon(
                    onPressed: controller.isSending.value
                        ? controller.cancelResponse
                        : controller.sendDraft,
                    icon: Icon(controller.isSending.value
                        ? Icons.stop_rounded
                        : Icons.send_rounded),
                    label: Text(controller.isSending.value ? '取消' : '发送'),
                  )),
            ],
          ),
        ],
      ),
    );
  }
}
