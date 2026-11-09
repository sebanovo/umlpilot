class Collaborator {
  const Collaborator({
    required this.userId,
    required this.email,
    required this.firstName,
    required this.lastName,
    required this.role,
    required this.invitationStatus,
  });

  factory Collaborator.fromJson(Map<String, dynamic> json) {
    return Collaborator(
      userId: (json['userId'] ?? '').toString(),
      email: (json['email'] ?? '').toString(),
      firstName: (json['firstName'] ?? '').toString(),
      lastName: (json['lastName'] ?? '').toString(),
      role: (json['role'] ?? 'viewer').toString(),
      invitationStatus: (json['invitationStatus'] ?? 'pending').toString(),
    );
  }

  final String userId;
  final String email;
  final String firstName;
  final String lastName;
  final String role;
  final String invitationStatus;

  bool get isPending => invitationStatus == 'pending';
  bool get isOwner => role == 'owner';
  bool get canEdit => role == 'owner' || role == 'editor';

  String get fullName => '$firstName $lastName'.trim().isEmpty ? email : '$firstName $lastName'.trim();
}
