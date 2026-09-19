import 'package:ainote_app/app/config/theme/my_colors.dart';
import 'package:ainote_app/app/utils/toast.dart';
import 'package:ainote_app/app/widgets/primary_btn.dart';
import 'package:board_datetime_picker/board_datetime_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_smart_dialog/flutter_smart_dialog.dart';

import '../utils/date_format.dart';
import '../utils/note.dart';
import 'secondary_btn.dart';

class TodoSheet extends StatefulWidget {
  final String? todoContent;
  final String? todoDesc;
  final DateTime? todoDate;
  final Function(String? todoContent, DateTime? todoDate, String? todoDesc)?
      onTodoCreated;
  final Function()? onDelete;
  final bool? isEdit;

  const TodoSheet(
      {super.key,
      this.todoContent,
      this.todoDesc,
      this.todoDate,
      this.onTodoCreated,
      this.isEdit = false,
      this.onDelete});

  @override
  State<TodoSheet> createState() => _TodoSheetState();
}

class _TodoSheetState extends State<TodoSheet> {
  final TextEditingController _todoContentTextController =
      TextEditingController();
  final TextEditingController _todoDescTextController = TextEditingController();
  final FocusNode _todoFocusNode = FocusNode();
  final FocusNode _todoDescFocusNode = FocusNode();
  String? _todoContent;
  String? _todoDesc;
  DateTime? _todoDate;

  @override
  void initState() {
    super.initState();
    _todoContent = widget.todoContent;
    _todoDesc = widget.todoDesc;
    _todoDate = widget.todoDate;
    _todoContentTextController.text = _todoContent ?? '';
    _todoDescTextController.text = _todoDesc ?? '';
  }

  @override
  void dispose() {
    _todoContentTextController.dispose();
    _todoFocusNode.dispose();
    _todoDescFocusNode.dispose();
    super.dispose();
  }

  _todoDateTapped() async {
    FocusScope.of(context).unfocus();
    final result = await showBoardDateTimePicker(
      context: context,
      pickerType: DateTimePickerType.datetime,
      options: const BoardDateTimeOptions(
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
    return Material(
      child: SafeArea(
        child: Padding(
          padding: EdgeInsets.only(
              top: 32.w,
              left: 16.w,
              right: 16.w,
              bottom: MediaQuery.of(context).viewInsets.bottom + 10.w),
          child: GestureDetector(
            behavior: HitTestBehavior.translucent,
            child: Container(
              decoration: BoxDecoration(
                borderRadius: BorderRadius.only(
                  topLeft: Radius.circular(20.w),
                  topRight: Radius.circular(20.w),
                ),
              ),
              child: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Padding(
                      padding: EdgeInsets.only(bottom: 16.0.w),
                      child: Text(
                        widget.isEdit == true ? '编辑待办' : '新增待办',
                        style: TextStyle(
                            color: Color(0xFF393640),
                            fontWeight: FontWeight.w700,
                            fontSize: 22.sp,
                            height: 1),
                      ),
                    ),
                    Padding(
                      padding: EdgeInsets.only(bottom: 16.0.w),
                      child: Container(
                        padding: const EdgeInsets.all(16.0),
                        decoration: BoxDecoration(
                          color: const Color(0xFFF3F5F8),
                          borderRadius: BorderRadius.circular(8.w),
                        ),
                        child: TextFormField(
                          autofocus: true,
                          controller: _todoContentTextController,
                          focusNode: _todoFocusNode,
                          style: TextStyle(
                            color: const Color(0xFF000000),
                            fontSize: 16.sp,
                            fontWeight: FontWeight.w400,
                            height: 1.2,
                          ),
                          maxLines: 3,
                          minLines: 1,
                          decoration: InputDecoration(
                            border: InputBorder.none,
                            hintText: '请输入待办内容',
                            hintStyle: TextStyle(
                              color: const Color(0xFF82818D).withOpacity(0.5),
                              fontSize: 16.sp,
                              fontWeight: FontWeight.w400,
                              height: 1.2,
                            ),
                          ),
                        ),
                      ),
                    ),
                    Padding(
                      padding: EdgeInsets.only(bottom: 16.0.w),
                      child: GestureDetector(
                        behavior: HitTestBehavior.translucent,
                        onTap: () {
                          _todoFocusNode.unfocus();
                          _todoDateTapped();
                        },
                        child: Container(
                            padding: EdgeInsets.all(16.0.w),
                            decoration: BoxDecoration(
                              color: const Color(0xFFF3F5F8),
                              borderRadius: BorderRadius.circular(8),
                            ),
                            width: double.infinity,
                            child: Text(
                              _todoDate == null
                                  ? "请选择待办时间"
                                  : formatDate(_todoDate!),
                              style: TextStyle(
                                color: _todoDate == null
                                    ? const Color(0xFF82818D).withOpacity(0.5)
                                    : const Color(0xFF000000),
                                fontSize: 16.sp,
                                fontWeight: FontWeight.w400,
                                height: 1.2,
                              ),
                            )),
                      ),
                    ),
                    Padding(
                      padding: EdgeInsets.only(bottom: 16.0.w),
                      child: Container(
                        padding: const EdgeInsets.all(16.0),
                        decoration: BoxDecoration(
                          color: const Color(0xFFF3F5F8),
                          borderRadius: BorderRadius.circular(8.w),
                        ),
                        child: TextFormField(
                          autofocus: true,
                          focusNode: _todoDescFocusNode,
                          controller: _todoDescTextController,
                          style: TextStyle(
                            color: const Color(0xFF000000),
                            fontSize: 16.sp,
                            fontWeight: FontWeight.w400,
                            height: 1.2,
                          ),
                          maxLines: 5,
                          minLines: 2,
                          decoration: InputDecoration(
                            border: InputBorder.none,
                            hintText: '请输入待办详情',
                            hintStyle: TextStyle(
                              color: const Color(0xFF82818D).withOpacity(0.5),
                              fontSize: 16.sp,
                              fontWeight: FontWeight.w400,
                              height: 1.2,
                            ),
                          ),
                        ),
                      ),
                    ),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Flexible(
                          child: PrimaryBtn(
                            margin: const EdgeInsets.only(bottom: 16.0),
                            text: '确认',
                            onPressed: () {
                              if (_todoContentTextController.text.isEmpty) {
                                Toast.info('请输入待办内容');
                                return;
                              }
                              // if (_todoDate == null) {
                              //   Toast.info('请输入待办时间');
                              //   return;
                              // }
                              setState(() {
                                _todoContent = _todoContentTextController.text;
                                _todoDesc = _todoDescTextController.text;
                              });
                              widget.onTodoCreated
                                  ?.call(_todoContent, _todoDate, _todoDesc);
                              Navigator.pop(context);
                            },
                          ),
                        ),
                        if (widget.isEdit == true) ...[
                          16.horizontalSpace,
                          Flexible(
                            child: SecondaryBtn(
                              backgroundColor: MyColors.colorDeleteRed,
                              margin: EdgeInsets.only(bottom: 16.0.w),
                              text: '删除',
                              textColor: MyColors.colorWhite,
                              onPressed: () {
                                _todoFocusNode.unfocus();
                                _todoDescFocusNode.unfocus();
                                confirmDelete(context, onDelete: () {
                                  widget.onDelete?.call();
                                  Navigator.pop(context);
                                });
                              },
                            ),
                          ),
                        ]
                      ],
                    )
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
