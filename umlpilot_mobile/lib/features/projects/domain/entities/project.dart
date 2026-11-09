class Project {
  const Project({
    required this.id,
    required this.name,
    required this.creatorId,
    required this.status,
    this.description,
    this.createdAt,
    this.updatedAt,
  });

  factory Project.fromJson(Map<String, dynamic> json) {
    return Project(
      id: (json['id'] ?? '').toString(),
      name: (json['name'] ?? '').toString(),
      description: json['description'] as String?,
      creatorId: (json['creatorId'] ?? '').toString(),
      status: (json['status'] ?? 'active').toString(),
      createdAt: json['createdAt'] as String?,
      updatedAt: json['updatedAt'] as String?,
    );
  }

  final String id;
  final String name;
  final String? description;
  final String creatorId;
  final String status;
  final String? createdAt;
  final String? updatedAt;
}
