import 'package:get/get.dart';

class AiAdditionalController extends GetxController {
  Map<int, String> selectedOptions = {};
  Map<int, String> customInputs = {};
  final currentIndex = 0.obs;
  final _selectedOptionsRx = RxMap<int, String>({});

  int get selectedIndex => currentIndex.value;

  @override
  void onInit() {
    super.onInit();
  }

  void updateSelectedIndex(int index) {
    currentIndex.value = index;
    update();
  }

  void updateSelectedOption(int index, String option) {
    selectedOptions[index] = option;
    _selectedOptionsRx[index] = option;
    update();
  }

  void updateCustomInput(int index, String input) {
    customInputs[index] = input;
    selectedOptions[index] = input;
    _selectedOptionsRx[index] = input;
    update();
  }

  String? getSelectedOption(int index) {
    return _selectedOptionsRx[index];
  }

  String? getCustomInput(int index) {
    return customInputs[index];
  }

  void handleOptionSelected(String option) {
    // 处理选项选择的逻辑
  }

  Map<String, String> getReplacedContent(List<String> originalWords) {
    Map<String, String> replacements = {};
    selectedOptions.forEach((index, option) {
      if (index < originalWords.length) {
        replacements[originalWords[index]] = option;
      }
    });
    return replacements;
  }

  bool hasSelectedOptions() {
    return selectedOptions.isNotEmpty;
  }
}
