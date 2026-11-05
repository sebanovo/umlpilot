import 'package:flutter/material.dart';

import '../../theme/app_colors.dart';
import '../../theme/app_spacing.dart';

class LoadingState extends StatelessWidget {
  const LoadingState({super.key, this.message});

  final String? message;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const CircularProgressIndicator(),
          if (message != null) ...[
            const SizedBox(height: AppSpacing.md),
            Text(message!, style: const TextStyle(color: AppColors.textSecondary)),
          ],
        ],
      ),
    );
  }
}
