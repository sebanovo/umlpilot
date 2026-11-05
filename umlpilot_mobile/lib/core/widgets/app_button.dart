import 'package:flutter/material.dart';

import '../../theme/app_colors.dart';
import '../../theme/app_spacing.dart';

enum AppButtonVariant { primary, secondary, danger, ghost }

class AppButton extends StatelessWidget {
  const AppButton({
    super.key,
    required this.label,
    this.onPressed,
    this.variant = AppButtonVariant.primary,
    this.icon,
    this.loading = false,
    this.expand = false,
  });

  final String label;
  final VoidCallback? onPressed;
  final AppButtonVariant variant;
  final IconData? icon;
  final bool loading;
  final bool expand;

  @override
  Widget build(BuildContext context) {
    final content = Row(
      mainAxisSize: MainAxisSize.min,
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        if (loading)
          const SizedBox(
            width: 16,
            height: 16,
            child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
          )
        else if (icon != null)
          Icon(icon, size: 18),
        if (loading || icon != null) const SizedBox(width: AppSpacing.sm),
        Text(label),
      ],
    );

    final shape = RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppRadius.lg));
    final size = Size(expand ? double.infinity : 0, 46);

    final Widget button = switch (variant) {
      AppButtonVariant.primary => FilledButton(
          style: FilledButton.styleFrom(
            minimumSize: size,
            backgroundColor: AppColors.primary,
            foregroundColor: Colors.white,
            shape: shape,
            textStyle: Theme.of(context).textTheme.labelLarge,
          ),
          onPressed: loading ? null : onPressed,
          child: content,
        ),
      AppButtonVariant.danger => FilledButton(
          style: FilledButton.styleFrom(
            minimumSize: size,
            backgroundColor: AppColors.danger,
            foregroundColor: Colors.white,
            shape: shape,
            textStyle: Theme.of(context).textTheme.labelLarge,
          ),
          onPressed: loading ? null : onPressed,
          child: content,
        ),
      AppButtonVariant.secondary => OutlinedButton(
          style: OutlinedButton.styleFrom(
            minimumSize: size,
            foregroundColor: AppColors.textPrimary,
            side: const BorderSide(color: AppColors.border),
            shape: shape,
            textStyle: Theme.of(context).textTheme.labelLarge,
          ),
          onPressed: loading ? null : onPressed,
          child: content,
        ),
      AppButtonVariant.ghost => TextButton(
          style: TextButton.styleFrom(
            minimumSize: size,
            foregroundColor: AppColors.textSecondary,
            shape: shape,
            textStyle: Theme.of(context).textTheme.labelLarge,
          ),
          onPressed: loading ? null : onPressed,
          child: content,
        ),
    };

    return expand ? SizedBox(width: double.infinity, child: button) : button;
  }
}
