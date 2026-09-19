import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
class AndroidProgressButton extends StatefulWidget {
  @override
  _AndroidProgressButtonState createState() => _AndroidProgressButtonState();
}
 
class _AndroidProgressButtonState extends State<AndroidProgressButton> {
  int _num = 2;
  Timer? timer;

  @override
  void initState(){
    super.initState();
    timer = Timer.periodic(const Duration(seconds: 1), (t) {
      setState(() {
        _num += 1;
      });
      if (_num >= 9) {
        t.cancel();
      }
    });
  }

  @override
  void dispose(){
    super.dispose();
    timer?.cancel();
  }
  
 
  @override
  Widget build(BuildContext context) {
    return Stack(
      alignment: AlignmentDirectional.center,
      children: <Widget>[
        Container(
          width: 1.sw,
          height: 30,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(50),
          ),
          child: Row(
            children: [
              Expanded(
                flex: _num,
                child: Container(
                  decoration: const BoxDecoration(
                    color: Color(0xff3A51FF),
                    borderRadius: BorderRadius.only(topLeft: Radius.circular(50), bottomLeft: Radius.circular(50)),
                  ),
                ),
              ),
              Expanded(
                flex: 10 - _num,
                child: Container(
                  decoration: const BoxDecoration(
                    color: Color(0x4d3A51FF),
                    borderRadius: BorderRadius.only(topRight: Radius.circular(50), bottomRight: Radius.circular(50)),
                  ),
                ),
              ),
            ],
          )
        ),
        const Positioned.fill(
          child: Center(child: Text("稍后可点击查看", style: TextStyle(color: Colors.white, fontSize: 12))),
        ),
      ],
    );
  }
}