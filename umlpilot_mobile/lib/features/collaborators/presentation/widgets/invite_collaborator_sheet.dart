import 'dart:async';

import 'package:flutter/material.dart';

import '../../../../core/constants/app_constants.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_snackbar.dart';
import '../../../../core/widgets/app_text_field.dart';
import '../../../../theme/app_colors.dart';
import '../../../../theme/app_spacing.dart';
import '../../domain/entities/user_search_result.dart';

class InviteCollaboratorSheet extends StatefulWidget {
  const InviteCollaboratorSheet({
    super.key,
    required this.searchUsers,
    required this.invite,
  });

  final Future<List<UserSearchResult>> Function(String query) searchUsers;
  final Future<String?> Function(String email, String role) invite;

  @override
  State<InviteCollaboratorSheet> createState() => _InviteCollaboratorSheetState();
}

class _InviteCollaboratorSheetState extends State<InviteCollaboratorSheet> {
  final _email = TextEditingController();
  Timer? _debounce;
  List<UserSearchResult> _results = const [];
  bool _searching = false;
  bool _submitting = false;
  String _role = 'editor';

  @override
  void dispose() {
    _debounce?.cancel();
    _email.dispose();
    super.dispose();
  }

  void _onChanged(String value) {
    _debounce?.cancel();
    final query = value.trim();
    if (query.length < AppConstants.userSearchMinChars) {
      setState(() {
        _results = const [];
        _searching = false;
      });
      return;
    }
    _debounce = Timer(const Duration(milliseconds: AppConstants.userSearchDebounceMs), () async {
      setState(() => _searching = true);
      final results = await widget.searchUsers(query);
      if (!mounted) return;
      setState(() {
        _results = results;
        _searching = false;
      });
    });
  }

  Future<void> _submit() async {
    final email = _email.text.trim();
    if (email.isEmpty || _submitting) return;
    setState(() => _submitting = true);
    final error = await widget.invite(email, _role);
    if (!mounted) return;
    setState(() => _submitting = false);
    if (error == null) {
      showAppSnackBar(context, 'Invitación enviada', isSuccess: true);
      Navigator.of(context).pop();
    } else {
      showAppSnackBar(context, error, isError: true);
    }
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
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text('Invitar por email', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: AppSpacing.sm),
          const Text(
            'El invitado debe tener cuenta en UMLPilot y recibirá la propuesta en su bandeja.',
            style: TextStyle(color: AppColors.textSecondary, fontSize: 13),
          ),
          const SizedBox(height: AppSpacing.lg),
          AppTextField(
            controller: _email,
            label: 'Email',
            hint: 'nombre@correo.com',
            keyboardType: TextInputType.emailAddress,
            prefixIcon: Icons.mail_outline,
            onChanged: _onChanged,
            suffixIcon: _searching
                ? const Padding(
                    padding: EdgeInsets.all(AppSpacing.md),
                    child: SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2)),
                  )
                : null,
          ),
          if (_results.isNotEmpty) ...[
            const SizedBox(height: AppSpacing.sm),
            Container(
              constraints: const BoxConstraints(maxHeight: 180),
              decoration: BoxDecoration(
                color: AppColors.surfaceAlt,
                borderRadius: BorderRadius.circular(AppRadius.lg),
                border: Border.all(color: AppColors.border),
              ),
              child: ListView.builder(
                shrinkWrap: true,
                itemCount: _results.length,
                itemBuilder: (context, index) {
                  final user = _results[index];
                  return ListTile(
                    dense: true,
                    title: Text(user.fullName),
                    subtitle: Text(user.email, style: const TextStyle(fontSize: 12)),
                    onTap: () {
                      setState(() {
                        _email.text = user.email;
                        _results = const [];
                      });
                    },
                  );
                },
              ),
            ),
          ],
          const SizedBox(height: AppSpacing.md),
          DropdownButtonFormField<String>(
            initialValue: _role,
            decoration: const InputDecoration(labelText: 'Rol'),
            items: const [
              DropdownMenuItem(value: 'editor', child: Text('Editor')),
              DropdownMenuItem(value: 'viewer', child: Text('Lector')),
            ],
            onChanged: (value) {
              if (value != null) setState(() => _role = value);
            },
          ),
          const SizedBox(height: AppSpacing.lg),
          AppButton(
            label: 'Enviar invitación',
            icon: Icons.person_add_alt,
            loading: _submitting,
            expand: true,
            onPressed: _submit,
          ),
        ],
      ),
    );
  }
}
