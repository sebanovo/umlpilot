String? validateRequired(String? value, {String label = 'Este campo'}) {
  if (value == null || value.trim().isEmpty) return '$label es obligatorio';
  return null;
}

String? validateEmail(String? value) {
  final required = validateRequired(value, label: 'El email');
  if (required != null) return required;
  final email = value!.trim();
  final regex = RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$');
  if (!regex.hasMatch(email)) return 'Ingresa un email válido';
  return null;
}

String? validatePassword(String? value) {
  final required = validateRequired(value, label: 'La contraseña');
  if (required != null) return required;
  if (value!.length < 8) return 'La contraseña debe tener al menos 8 caracteres';
  return null;
}
