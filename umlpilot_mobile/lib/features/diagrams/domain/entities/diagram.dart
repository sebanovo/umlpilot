class Diagram {
  const Diagram({
    required this.id,
    required this.projectId,
    required this.name,
    required this.type,
    required this.version,
    required this.isLocked,
    this.description,
    this.lockedByUserId,
    this.createdAt,
    this.updatedAt,
  });

  factory Diagram.fromJson(Map<String, dynamic> json) {
    return Diagram(
      id: (json['id'] ?? '').toString(),
      projectId: (json['projectId'] ?? '').toString(),
      name: (json['name'] ?? '').toString(),
      type: (json['type'] ?? 'class').toString(),
      description: json['description'] as String?,
      version: json['version'] is num ? (json['version'] as num).toInt() : 1,
      isLocked: json['isLocked'] == true,
      lockedByUserId: json['lockedByUserId'] as String?,
      createdAt: json['createdAt'] as String?,
      updatedAt: json['updatedAt'] as String?,
    );
  }

  final String id;
  final String projectId;
  final String name;
  final String type;
  final String? description;
  final int version;
  final bool isLocked;
  final String? lockedByUserId;
  final String? createdAt;
  final String? updatedAt;
}
