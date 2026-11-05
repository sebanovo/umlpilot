import 'package:flutter/material.dart';

import '../../theme/app_colors.dart';

class AppTextField extends StatelessWidget {
  const AppTextField({
    super.key,
    this.controller,
    this.label,
    this.hint,
    this.validator,
    this.obscureText = false,
    this.enabled = true,
    this.keyboardType,
    this.maxLines = 1,
    this.prefixIcon,
    this.suffixIcon,
    this.onChanged,
    this.textInputAction,
    this.autofocus = false,
  });

  final TextEditingController? controller;
  final String? label;
  final String? hint;
  final String? Function(String?)? validator;
  final bool obscureText;
  final bool enabled;
  final TextInputType? keyboardType;
  final int maxLines;
  final IconData? prefixIcon;
  final Widget? suffixIcon;
  final ValueChanged<String>? onChanged;
  final TextInputAction? textInputAction;
  final bool autofocus;

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: controller,
      validator: validator,
      obscureText: obscureText,
      enabled: enabled,
      keyboardType: keyboardType,
      maxLines: obscureText ? 1 : maxLines,
      onChanged: onChanged,
      textInputAction: textInputAction,
      autofocus: autofocus,
      style: const TextStyle(color: AppColors.textPrimary),
      decoration: InputDecoration(
        labelText: label,
        hintText: hint,
        prefixIcon: prefixIcon != null ? Icon(prefixIcon, size: 20, color: AppColors.textMuted) : null,
        suffixIcon: suffixIcon,
      ),
    );
  }
}
