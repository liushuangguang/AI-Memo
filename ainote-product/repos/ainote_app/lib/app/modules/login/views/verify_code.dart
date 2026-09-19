import 'package:ainote_app/app/modules/login/views/widgets/VerifyCode.dart';
import 'package:flutter/material.dart';

import 'widgets/LoginWrap.dart';

class VerifyCodePage extends StatelessWidget {
  const VerifyCodePage({super.key});

  @override
  Widget build(BuildContext context) {
    return const LoginWrap(child: VerifyCode());
  }
}
