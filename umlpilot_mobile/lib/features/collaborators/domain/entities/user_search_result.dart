class UserSearchResult {
  const UserSearchResult({
    required this.id,
    required this.email,
    required this.firstName,
    required this.lastName,
  });

  factory UserSearchResult.fromJson(Map<String, dynamic> json) {
    return UserSearchResult(
      id: (json['id'] ?? '').toString(),
      email: (json['email'] ?? '').toString(),
      firstName: (json['firstName'] ?? '').toString(),
      lastName: (json['lastName'] ?? '').toString(),
    );
  }

  final String id;
  final String email;
  final String firstName;
  final String lastName;

  String get fullName => '$firstName $lastName'.trim().isEmpty ? email : '$firstName $lastName'.trim();
}
