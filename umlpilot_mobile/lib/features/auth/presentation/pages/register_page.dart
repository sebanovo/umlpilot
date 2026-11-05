import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../../core/utils/validators.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_text_field.dart';
import '../../../../theme/app_colors.dart';
import '../../../../theme/app_spacing.dart';
import '../viewmodels/auth_viewmodel.dart';
import '../widgets/auth_scaffold.dart';

class RegisterPage extends StatefulWidget {
  const RegisterPage({super.key});

  @override
  State<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends State<RegisterPage> {
  final _formKey = GlobalKey<FormState>();
  final _firstName = TextEditingController();
  final _lastName = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();
  bool _obscure = true;

  @override
  void dispose() {
    _firstName.dispose();
    _lastName.dispose();
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  String? get _from => GoRouterState.of(context).uri.queryParameters['from'];

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    final viewModel = context.read<AuthViewModel>();
    final ok = await viewModel.register(
      email: _email.text.trim(),
      password: _password.text,
      firstName: _firstName.text.trim(),
      lastName: _lastName.text.trim(),
    );
    if (!mounted || !ok) return;
    final from = _from;
    context.go(from != null && from.isNotEmpty ? from : '/projects');
  }

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<AuthViewModel>();
    final from = _from;

    return AuthScaffold(
      title: 'Crea tu cuenta',
      subtitle: 'Empieza a modelar tus diagramas UML desde el móvil.',
      child: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Expanded(
                  child: AppTextField(
                    controller: _firstName,
                    label: 'Nombre',
                    textInputAction: TextInputAction.next,
                    validator: (value) => validateRequired(value, label: 'El nombre'),
                  ),
                ),
                const SizedBox(width: AppSpacing.md),
                Expanded(
                  child: AppTextField(
                    controller: _lastName,
                    label: 'Apellido',
                    textInputAction: TextInputAction.next,
                    validator: (value) => validateRequired(value, label: 'El apellido'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: AppSpacing.md),
            AppTextField(
              controller: _email,
              label: 'Email',
              hint: 'nombre@correo.com',
              prefixIcon: Icons.mail_outline,
              keyboardType: TextInputType.emailAddress,
              textInputAction: TextInputAction.next,
              validator: validateEmail,
            ),
            const SizedBox(height: AppSpacing.md),
            AppTextField(
              controller: _password,
              label: 'Contraseña',
              prefixIcon: Icons.lock_outline,
              obscureText: _obscure,
              textInputAction: TextInputAction.done,
              validator: validatePassword,
              suffixIcon: IconButton(
                tooltip: _obscure ? 'Mostrar contraseña' : 'Ocultar contraseña',
                icon: Icon(
                  _obscure ? Icons.visibility_outlined : Icons.visibility_off_outlined,
                  color: AppColors.textMuted,
                ),
                onPressed: () => setState(() => _obscure = !_obscure),
              ),
            ),
            if (viewModel.error != null) ...[
              const SizedBox(height: AppSpacing.md),
              Text(viewModel.error!, style: const TextStyle(color: AppColors.danger)),
            ],
            const SizedBox(height: AppSpacing.xl),
            AppButton(
              label: 'Crear cuenta',
              icon: Icons.person_add_alt,
              loading: viewModel.loading,
              expand: true,
              onPressed: _submit,
            ),
            const SizedBox(height: AppSpacing.md),
            TextButton(
              onPressed: () {
                final target = from != null && from.isNotEmpty ? '?from=${Uri.encodeComponent(from)}' : '';
                context.go('/login$target');
              },
              child: const Text('Ya tengo cuenta'),
            ),
          ],
        ),
      ),
    );
  }
}
