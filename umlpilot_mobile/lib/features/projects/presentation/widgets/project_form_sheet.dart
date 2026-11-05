import 'package:flutter/material.dart';

import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_text_field.dart';
import '../../../../theme/app_spacing.dart';

class ProjectFormData {
  const ProjectFormData({required this.name, this.description});

  final String name;
  final String? description;
}

class ProjectFormSheet extends StatefulWidget {
  const ProjectFormSheet({
    super.key,
    this.title = 'Nuevo proyecto',
    this.initialName = '',
    this.initialDescription = '',
    this.submitLabel = 'Crear',
  });

  final String title;
  final String initialName;
  final String initialDescription;
  final String submitLabel;

  @override
  State<ProjectFormSheet> createState() => _ProjectFormSheetState();
}

class _ProjectFormSheetState extends State<ProjectFormSheet> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _name = TextEditingController(text: widget.initialName);
  late final TextEditingController _description = TextEditingController(text: widget.initialDescription);

  @override
  void dispose() {
    _name.dispose();
    _description.dispose();
    super.dispose();
  }

  void _submit() {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    Navigator.of(context).pop(
      ProjectFormData(
        name: _name.text.trim(),
        description: _description.text.trim().isEmpty ? null : _description.text.trim(),
      ),
    );
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
            Text(widget.title, style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: AppSpacing.lg),
            AppTextField(
              controller: _name,
              label: 'Nombre',
              hint: 'Mi proyecto UML',
              textInputAction: TextInputAction.next,
              validator: (value) =>
                  value == null || value.trim().isEmpty ? 'El nombre es obligatorio' : null,
            ),
            const SizedBox(height: AppSpacing.md),
            AppTextField(
              controller: _description,
              label: 'Descripción (opcional)',
              hint: '¿De qué trata el proyecto?',
              maxLines: 3,
            ),
            const SizedBox(height: AppSpacing.xl),
            AppButton(label: widget.submitLabel, expand: true, onPressed: _submit),
          ],
        ),
      ),
    );
  }
}
