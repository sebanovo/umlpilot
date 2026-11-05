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

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  final _email = TextEditingController();
  final _password = TextEditingController();
  bool _obscure = true;

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  String? get _from => GoRouterState.of(context).uri.queryParameters['from'];

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    final viewModel = context.read<AuthViewModel>();
    final ok = await viewModel.login(email: _email.text.trim(), password: _password.text);
    if (!mounted || !ok) return;
    final from = _from;
    context.go(from != null && from.isNotEmpty ? from : '/projects');
  }

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<AuthViewModel>();
    final from = _from;

    return AuthScaffold(
      title: 'Inicia sesión',
      subtitle: 'Vuelve a tus diagramas y continúa donde lo dejaste.',
      child: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
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
              label: 'Entrar',
              icon: Icons.login,
              loading: viewModel.loading,
              expand: true,
              onPressed: _submit,
            ),
            const SizedBox(height: AppSpacing.md),
            TextButton(
              onPressed: () {
                final target = from != null && from.isNotEmpty ? '?from=${Uri.encodeComponent(from)}' : '';
                context.go('/register$target');
              },
              child: const Text('¿No tienes cuenta? Crear una'),
            ),
          ],
        ),
      ),
    );
  }
}
