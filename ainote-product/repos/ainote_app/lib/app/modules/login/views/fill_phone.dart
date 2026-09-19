import 'package:ainote_app/app/modules/login/controllers/login_controller.dart';
import 'package:ainote_app/app/modules/login/views/widgets/PhoneForm.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

import 'widgets/LoginWrap.dart';

class FillPhone extends StatelessWidget {
  const FillPhone({super.key});

  @override
  Widget build(BuildContext context) {
    return LoginWrap(child: PhoneForm());
  }
}
