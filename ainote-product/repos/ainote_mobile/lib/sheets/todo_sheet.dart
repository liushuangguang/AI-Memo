import 'package:board_datetime_picker/board_datetime_picker.dart';
import 'package:flutter/material.dart';
import 'package:fluttertoast/fluttertoast.dart';

import '../constants/app_colors.dart';
import '../utils/utils.dart';

class TodoSheet extends StatefulWidget {
  final String? todoContent;
  final DateTime? todoDate;
  final Function(String? todoContent, DateTime? todoDate)? onTodoCreated;
  final bool? isEdit;
  const TodoSheet({super.key, this.todoContent, this.todoDate, this.onTodoCreated, this.isEdit = false});

  @override
  State<TodoSheet> createState() => _TodoSheetState();
}

class _TodoSheetState extends State<TodoSheet> {
  final TextEditingController _todoContentTextController = TextEditingController();
  final FocusNode _todoFocusNode = FocusNode();
  String? _todoContent;
  DateTime? _todoDate;

  @override
  void initState() {
    super.initState();
    _todoContent = widget.todoContent;
    _todoDate = widget.todoDate;
    _todoContentTextController.text = _todoContent ?? '';
  }

  @override
  void dispose() {
    _todoContentTextController.dispose();
    _todoFocusNode.dispose();
    super.dispose();
  }

  _todoDateTapped() async {
    FocusScope.of(context).unfocus();
    final result = await showBoardDateTimePicker(
      context: context,
      pickerType: DateTimePickerType.datetime,
      options:  const BoardDateTimeOptions(
        languages: BoardPickerLanguages.en(),
        inputable: false,
      ),
    );
    if (result != null) {
      setState(() {
        _todoDate = result;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: EdgeInsets.only(
            top: 32, left: 16, right: 16, bottom: MediaQuery
            .of(context)
            .viewInsets
            .bottom + 10),
        child: GestureDetector(
          behavior: HitTestBehavior.translucent,
          onTap: () {
            _todoFocusNode.unfocus();
          },
          child: Container(
            decoration: const BoxDecoration(
              color: AppColors.background,
              borderRadius: BorderRadius.only(
                topLeft: Radius.circular(20),
                topRight: Radius.circular(20),
              ),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Padding(
                  padding: const EdgeInsets.only(bottom: 16.0),
                  child: Text(
                    widget.isEdit == true ? '编辑待办' : '新增待办',
                    style: TextStyle(
                        color: Color(0xFF393640),
                        fontWeight: FontWeight.w700,
                        fontSize: 22,
                        height: 1
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.only(bottom: 16.0),
                  child: Container(
                    padding: const EdgeInsets.all(16.0),
                    decoration: BoxDecoration(
                      color: const Color(0xFFF3F5F8),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: TextField(
                      autofocus: false,
                      controller: _todoContentTextController,
                      focusNode: _todoFocusNode,
                      style: TextStyle(
                        color: const Color(0xFF000000),
                        fontSize: 16,
                        fontWeight: FontWeight.w400,
                        height: 1.2,
                      ),
                      maxLines: 2,
                      decoration: InputDecoration(
                        border: InputBorder.none,
                        hintText: '请输入待办内容',
                        hintStyle: TextStyle(
                          color: const Color(0xFF82818D).withOpacity(0.5),
                          fontSize: 16,
                          fontWeight: FontWeight.w400,
                          height: 1.2,
                        ),
                      ),
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.only(bottom: 16.0),
                  child: GestureDetector(
                    behavior: HitTestBehavior.translucent,
                    onTap: () {
                      _todoDateTapped();
                    },
                    child: Container(
                        padding: const EdgeInsets.all(16.0),
                        decoration: BoxDecoration(
                          color: const Color(0xFFF3F5F8),
                          borderRadius: BorderRadius.circular(8),
                        ),
                        width: double.infinity,
                        child: Text(
                          _todoDate == null
                              ? "请输入待办时间"
                              : formatDateTimeFromDateTime(_todoDate!, showYear: true),
                          style: TextStyle(
                            color: _todoDate == null ? const Color(0xFF82818D)
                                .withOpacity(0.5) : const Color(0xFF000000),
                            fontSize: 16,
                            fontWeight: FontWeight.w400,
                            height: 1.2,
                          ),
                        )
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.only(bottom: 16.0),
                  child: GestureDetector(
                    behavior: HitTestBehavior.translucent,
                    onTap: () {
                      if (_todoContentTextController.text.isEmpty) {
                        Fluttertoast.showToast(
                          msg: '请输入待办内容',
                          toastLength: Toast.LENGTH_SHORT,
                          gravity: ToastGravity.CENTER,
                          timeInSecForIosWeb: 1,
                          backgroundColor: const Color(0xFF000000),
                          textColor: const Color(0xFFFFFFFF),
                          fontSize: 16.0,
                        );
                        return;
                      }
                      setState(() {
                        _todoContent = _todoContentTextController.text;
                      });
                      widget.onTodoCreated?.call(_todoContent, _todoDate);
                      Navigator.pop(context);
                    },
                    child: Container(
                      height: 54,
                      decoration: BoxDecoration(
                        color: Color(0xFF12102F),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Center(
                        child: Text(
                          '确定',
                          style: TextStyle(
                            color: Color(0xFFFDFCFF),
                            fontWeight: FontWeight.w700,
                            fontSize: 16,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
                GestureDetector(
                  behavior: HitTestBehavior.translucent,
                  onTap: () {
                    Navigator.pop(context);
                  },
                  child: Container(
                    height: 54,
                    decoration: BoxDecoration(
                      color: Color(0xFFDFDEE8),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Center(
                      child: Text(
                        '取消',
                        style: TextStyle(
                          color: Color(0xFF12102F),
                          fontWeight: FontWeight.w700,
                          fontSize: 16,
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
