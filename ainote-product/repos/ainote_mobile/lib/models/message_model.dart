import 'dart:convert';

import 'package:ainote/models/todo_model.dart';
import 'package:animate_do/animate_do.dart';
import 'package:intl/intl.dart';
import 'package:ainote/screens/ai_voice_discuss/message/message_item.dart';
import 'package:flutter/material.dart';
import 'package:json_annotation/json_annotation.dart';
import 'package:uuid/uuid.dart';

import '../constants/app_images.dart';
import '../screens/widgets/ai_result_item.dart';
import '../screens/widgets/todo_result_item.dart';

@JsonSerializable()
class MessageModel {
  String? id;
  final String content;
  String? ct;
  final int msgType;
  final int userType;
  final Function? onTap;

  MessageModel({
    required this.content,
    this.msgType = 0,
    this.userType = 0,
    this.onTap,
  })  : id = const Uuid().v4(),
        ct = DateFormat('yyyy-MM-dd HH:mm:ss').format(DateTime.now());

  Map<String, dynamic> toJson() => {
        'id': id,
        'content': content,
        'date': ct,
        'msgType': msgType,
        'userType': userType,
      };

  Widget toWidget() {
    switch (msgType) {
      case 2:
        return FadeInUp(
          duration: const Duration(milliseconds: 500),
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 8.0),
            child: ResultItem(
                loading: false,
                backgroundColor: const Color(0xFFFFFDF4),
                titleColor: const Color(0xFF624E00),
                header: Row(children: [
                  ResultItemHeader(
                    title: '备忘录信息',
                    titleColor: const Color(0xFF624E00),
                    iconPath: AppImages.pinIcon.path,
                  ),
                  const Spacer(),
                  ResultItemAction(
                    actionText: '存入正文',
                    afterActionText: '存入成功',
                    onSave: () {
                      onTap?.call();
                    },
                  ),
                ]),
                text: content),
          ),
        );
      case 3:
        // todo bug fix
        final list = jsonDecode(content);
        final result = list.map((e) => TodoModel.fromJson(e)).toList();
        return FadeInUp(
          duration: const Duration(milliseconds: 500),
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 8.0),
            child: TodoResultItem(
                todos: result,
                onSave: (todos) {
                  onTap?.call(todos);
                  return Future(() => true);
                }),
          ),
        );
      default:
        switch (userType) {
          case 1:
            return MessageItem(message: content, isAiMessage: true);
          case 2:
            return MessageItem(message: content, isAiMessage: false);
          default:
            return MessageItem(message: content, isAiMessage: true);
        }
    }
  }

  factory MessageModel.fromJson(Map<String, dynamic> json) {
    MessageModel model = MessageModel(
      content: json['content'] ?? '',
      msgType: json['msgType'] ?? 0,
      userType: json['userType'] ?? 0,
    );
    model.id = json['id'];
    model.ct = json['ct'];
    return model;
  }

  @override
  String toString() {
    return 'MessageModel{id: $id, content: $content, date: $ct, msgType: $msgType, userType: $userType}';
  }
}
