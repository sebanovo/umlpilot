class AppNotification {
  const AppNotification({
    required this.id,
    required this.type,
    required this.title,
    required this.message,
    required this.read,
    required this.createdAt,
    this.projectId,
    this.role,
    this.readAt,
    this.invitationStatus,
  });

  factory AppNotification.fromJson(Map<String, dynamic> json) {
    return AppNotification(
      id: (json['id'] ?? '').toString(),
      type: (json['type'] ?? '').toString(),
      title: (json['title'] ?? '').toString(),
      message: (json['message'] ?? '').toString(),
      projectId: json['projectId'] as String?,
      role: json['role'] as String?,
      read: json['read'] == true,
      readAt: json['readAt'] as String?,
      createdAt: (json['createdAt'] ?? '').toString(),
      invitationStatus: json['invitationStatus'] as String?,
    );
  }

  final String id;
  final String type;
  final String title;
  final String message;
  final String? projectId;
  final String? role;
  final bool read;
  final String? readAt;
  final String createdAt;
  final String? invitationStatus;

  bool get isPendingInvitation => invitationStatus == 'pending';
}
