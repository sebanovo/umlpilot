class User {
  const User({
    required this.id,
    required this.email,
    required this.firstName,
    required this.lastName,
    required this.role,
  });

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: (json['id'] ?? json['userId'] ?? '').toString(),
      email: (json['email'] ?? '').toString(),
      firstName: (json['firstName'] ?? '').toString(),
      lastName: (json['lastName'] ?? '').toString(),
      role: (json['role'] ?? 'USER').toString(),
    );
  }

  final String id;
  final String email;
  final String firstName;
  final String lastName;
  final String role;

  String get fullName => '$firstName $lastName'.trim().isEmpty ? email : '$firstName $lastName'.trim();

  Map<String, dynamic> toJson() => {
        'id': id,
        'email': email,
        'firstName': firstName,
        'lastName': lastName,
        'role': role,
      };
}
