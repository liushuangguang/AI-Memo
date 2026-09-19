Future<Stream<String>> mockAIGenerate() async {
  return Stream.periodic(Duration(seconds: 2), (index) {
    return '你好${index + 1}，我是AI，请问有什么可以帮到你的？';
  });
}

Future<Stream<String>> mockAIGenerateOne1() async {
  return Stream.value('你好，我是你的智能AI助理');
}

Future<Stream<String>> mockAIGenerateOne2() async {
  return Stream.value('AI助理你好！很高兴认识你');
}
