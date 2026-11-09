class ShareLinkState {
  const ShareLinkState({
    required this.enabled,
    required this.role,
    this.token,
    this.url,
  });

  factory ShareLinkState.fromJson(Map<String, dynamic> json) {
    return ShareLinkState(
      enabled: json['enabled'] == true,
      token: json['token'] as String?,
      url: json['url'] as String?,
      role: (json['role'] ?? 'viewer').toString(),
    );
  }

  final bool enabled;
  final String? token;
  final String? url;
  final String role;
}

class ShareJoinResult {
  const ShareJoinResult({
    required this.projectId,
    required this.diagramId,
    required this.role,
  });

  factory ShareJoinResult.fromJson(Map<String, dynamic> json) {
    return ShareJoinResult(
      projectId: (json['projectId'] ?? '').toString(),
      diagramId: (json['diagramId'] ?? '').toString(),
      role: (json['role'] ?? 'viewer').toString(),
    );
  }

  final String projectId;
  final String diagramId;
  final String role;
}
