import 'package:flutter/material.dart';

/// Indicador de carga reutilizable, con etiqueta opcional.
class LoadingWidget extends StatelessWidget {
  const LoadingWidget({super.key, this.label});

  final String? label;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        const SizedBox(
          width: 16,
          height: 16,
          child: CircularProgressIndicator(strokeWidth: 2),
        ),
        if (label != null) ...[
          const SizedBox(width: 10),
          Text(label!, style: Theme.of(context).textTheme.bodyMedium),
        ],
      ],
    );
  }
}
