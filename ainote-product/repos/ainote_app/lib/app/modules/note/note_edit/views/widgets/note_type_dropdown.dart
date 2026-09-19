import 'package:auto_size_text/auto_size_text.dart';
import 'package:dropdown_search/dropdown_search.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

import '../../../../../config/theme/my_colors.dart';
import '../../../../../data/models/note_type_model.dart';

NoteTypeModel? resolveNoteTypeSelection(
  Iterable<NoteTypeModel> options,
  int? selectedId,
) {
  if (selectedId == null) return null;
  for (final option in options) {
    if (option.id == selectedId) return option;
  }
  return null;
}

class NoteTagDropdown extends StatefulWidget {
  final int? value;
  final List<NoteTypeModel> list;
  final Function(NoteTypeModel) onChang;
  final bool? disabled;
  final bool loading;
  final DropdownSearchBuilder? dropdownBuilder;

  const NoteTagDropdown(
      {super.key,
      required this.list,
      required this.onChang,
      this.value,
      this.dropdownBuilder,
      this.disabled,
      this.loading = false});

  @override
  State<NoteTagDropdown> createState() => _NoteTagDropdownState();
}

class _NoteTagDropdownState extends State<NoteTagDropdown> {
  NoteTypeModel? selectedItem;

  @override
  void initState() {
    super.initState();
    _syncSelection();
  }

  @override
  void didUpdateWidget(covariant NoteTagDropdown oldWidget) {
    super.didUpdateWidget(oldWidget);
    _syncSelection();
  }

  void _syncSelection() {
    selectedItem = resolveNoteTypeSelection(widget.list, widget.value);
  }

  @override
  Widget build(BuildContext context) {
    int len = widget.list.length;
    final canSelect =
        widget.disabled != true && !widget.loading && widget.list.isNotEmpty;
    return DropdownSearch<NoteTypeModel>(
      enabled: canSelect,
      selectedItem: selectedItem,
      mode: Mode.custom,
      items: (f, cs) => widget.list,
      compareFn: (item1, item2) => item1.id == item2.id,
      onChanged: (value) {
        if (canSelect) {
          final resolved = resolveNoteTypeSelection(widget.list, value?.id);
          if (resolved == null) return;
          setState(() {
            selectedItem = resolved;
          });
          widget.onChang(resolved);
        }
      },
      popupProps: PopupProps.menu(
        fit: FlexFit.loose,
        itemBuilder: (context, item, isDisabled, isSelected) {
          if (widget.disabled == true) return Container();
          return Padding(
            padding: EdgeInsets.symmetric(vertical: 8.0.w, horizontal: 12.w),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.start,
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 4.w,
                  height: 12.h,
                  margin: EdgeInsets.only(right: 8.w, left: 8.w, top: 6.w),
                  decoration: BoxDecoration(
                    color: MyColors.colorBlue,
                    borderRadius: BorderRadius.circular(10.w),
                  ),
                ),
                Expanded(
                  child: AutoSizeText(
                    item.name,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(fontSize: 14.sp),
                    textAlign: TextAlign.left,
                  ),
                ),
                // Spacer(),
                if (selectedItem?.id == item.id)
                  Icon(
                    Icons.check_rounded,
                    size: 16.sp,
                    color: MyColors.colorBlue,
                  )
              ],
            ),
          );
        },
        constraints: BoxConstraints(
          maxHeight: len == 0 ? 48.h : (len < 8 ? len * 40.h : 380.h),
        ),
        menuProps: MenuProps(
          margin: EdgeInsets.only(top: 8.h),
          shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.all(Radius.circular(12.w))),
        ),
      ),
      dropdownBuilder: widget.dropdownBuilder ??
          (ctx, selectedItem) => Padding(
                padding: EdgeInsets.only(left: 8.w, right: 8.w, top: 16.h),
                child: Center(
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.center,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Expanded(
                          child: AutoSizeText(
                        selectedItem?.name ?? (widget.loading ? '加载中…' : '未分类'),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                            fontSize: 14.sp, color: MyColors.colorBrown),
                        textAlign: TextAlign.center,
                      )),
                      if (canSelect)
                        Icon(
                          Icons.arrow_drop_down,
                          color: MyColors.colorBrown,
                        ),
                    ],
                  ),
                ),
              ),
    );
  }
}
