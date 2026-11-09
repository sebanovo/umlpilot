import 'package:flutter/material.dart';

import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_text_field.dart';
import '../../../../theme/app_spacing.dart';

class DiagramFormData {
  const DiagramFormData({required this.name, required this.type});

  final String name;
  final String type;
}

class DiagramFormSheet extends StatefulWidget {
  const DiagramFormSheet({super.key});

  @override
  State<DiagramFormSheet> createState() => _DiagramFormSheetState();
}

class _DiagramFormSheetState extends State<DiagramFormSheet> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  String _type = 'class';

  static const _types = <String, String>{
    'class': 'Clases',
    'sequence': 'Secuencia',
    'use_case': 'Casos de uso',
    'activity': 'Actividad',
    'state': 'Estados',
  };

  @override
  void dispose() {
    _name.dispose();
    super.dispose();
  }

  void _submit() {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    Navigator.of(context).pop(DiagramFormData(name: _name.text.trim(), type: _type));
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(
        AppSpacing.lg,
        AppSpacing.lg,
        AppSpacing.lg,
        MediaQuery.of(context).viewInsets.bottom + AppSpacing.lg,
      ),
      child: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('Nuevo diagrama', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: AppSpacing.lg),
            AppTextField(
              controller: _name,
              label: 'Nombre',
              hint: 'Modelo de dominio',
              textInputAction: TextInputAction.done,
              validator: (value) =>
                  value == null || value.trim().isEmpty ? 'El nombre es obligatorio' : null,
            ),
            const SizedBox(height: AppSpacing.md),
            DropdownButtonFormField<String>(
              initialValue: _type,
              decoration: const InputDecoration(labelText: 'Tipo'),
              items: _types.entries
                  .map((entry) => DropdownMenuItem(value: entry.key, child: Text(entry.value)))
                  .toList(),
              onChanged: (value) {
                if (value != null) setState(() => _type = value);
              },
            ),
            const SizedBox(height: AppSpacing.xl),
            AppButton(label: 'Crear diagrama', expand: true, onPressed: _submit),
          ],
        ),
      ),
    );
  }
}
