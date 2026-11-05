class UmlElement {
  const UmlElement({
    required this.id,
    required this.diagramId,
    required this.name,
    required this.visibility,
    required this.elementType,
    required this.positionX,
    required this.positionY,
    required this.width,
    required this.height,
    this.stereotype,
  });

  factory UmlElement.fromJson(Map<String, dynamic> json) {
    return UmlElement(
      id: (json['id'] ?? '').toString(),
      diagramId: (json['diagramId'] ?? '').toString(),
      name: (json['name'] ?? '').toString(),
      visibility: (json['visibility'] ?? 'public').toString(),
      stereotype: json['stereotype'] as String?,
      elementType: (json['elementType'] ?? 'class').toString(),
      positionX: json['positionX'] is num ? (json['positionX'] as num).toInt() : 0,
      positionY: json['positionY'] is num ? (json['positionY'] as num).toInt() : 0,
      width: json['width'] is num ? (json['width'] as num).toInt() : 180,
      height: json['height'] is num ? (json['height'] as num).toInt() : 100,
    );
  }

  final String id;
  final String diagramId;
  final String name;
  final String visibility;
  final String? stereotype;
  final String elementType;
  final int positionX;
  final int positionY;
  final int width;
  final int height;

  UmlElement copyWith({
    String? name,
    String? visibility,
    String? stereotype,
    int? positionX,
    int? positionY,
    int? width,
    int? height,
  }) {
    return UmlElement(
      id: id,
      diagramId: diagramId,
      name: name ?? this.name,
      visibility: visibility ?? this.visibility,
      stereotype: stereotype ?? this.stereotype,
      elementType: elementType,
      positionX: positionX ?? this.positionX,
      positionY: positionY ?? this.positionY,
      width: width ?? this.width,
      height: height ?? this.height,
    );
  }
}

class UmlAttribute {
  const UmlAttribute({
    required this.id,
    required this.elementId,
    required this.name,
    required this.dataType,
    required this.visibility,
    required this.orderIndex,
    this.alias,
    this.stereotype,
    this.defaultValue,
  });

  factory UmlAttribute.fromJson(Map<String, dynamic> json) {
    return UmlAttribute(
      id: (json['id'] ?? '').toString(),
      elementId: (json['elementId'] ?? '').toString(),
      name: (json['name'] ?? '').toString(),
      dataType: (json['dataType'] ?? 'String').toString(),
      visibility: (json['visibility'] ?? 'private').toString(),
      alias: json['alias'] as String?,
      stereotype: json['stereotype'] as String?,
      defaultValue: json['defaultValue'] as String?,
      orderIndex: json['orderIndex'] is num ? (json['orderIndex'] as num).toInt() : 0,
    );
  }

  final String id;
  final String elementId;
  final String name;
  final String dataType;
  final String visibility;
  final String? alias;
  final String? stereotype;
  final String? defaultValue;
  final int orderIndex;
}

class UmlParameter {
  const UmlParameter({
    required this.id,
    required this.methodId,
    required this.name,
    required this.dataType,
    required this.orderIndex,
    this.direction,
    this.notes,
  });

  factory UmlParameter.fromJson(Map<String, dynamic> json) {
    return UmlParameter(
      id: (json['id'] ?? '').toString(),
      methodId: (json['methodId'] ?? '').toString(),
      name: (json['name'] ?? '').toString(),
      dataType: (json['dataType'] ?? 'String').toString(),
      direction: json['direction'] as String?,
      notes: json['notes'] as String?,
      orderIndex: json['orderIndex'] is num ? (json['orderIndex'] as num).toInt() : 0,
    );
  }

  final String id;
  final String methodId;
  final String name;
  final String dataType;
  final String? direction;
  final String? notes;
  final int orderIndex;
}

class UmlMethod {
  const UmlMethod({
    required this.id,
    required this.elementId,
    required this.name,
    required this.returnType,
    required this.visibility,
    required this.orderIndex,
    required this.parameters,
    this.alias,
    this.stereotype,
    this.defaultValue,
  });

  factory UmlMethod.fromJson(Map<String, dynamic> json) {
    return UmlMethod(
      id: (json['id'] ?? '').toString(),
      elementId: (json['elementId'] ?? '').toString(),
      name: (json['name'] ?? '').toString(),
      returnType: (json['returnType'] ?? 'void').toString(),
      visibility: (json['visibility'] ?? 'public').toString(),
      alias: json['alias'] as String?,
      stereotype: json['stereotype'] as String?,
      defaultValue: json['defaultValue'] as String?,
      orderIndex: json['orderIndex'] is num ? (json['orderIndex'] as num).toInt() : 0,
      parameters: (json['parameters'] as List? ?? const [])
          .map((item) => UmlParameter.fromJson((item as Map).cast<String, dynamic>()))
          .toList(),
    );
  }

  final String id;
  final String elementId;
  final String name;
  final String returnType;
  final String visibility;
  final String? alias;
  final String? stereotype;
  final String? defaultValue;
  final int orderIndex;
  final List<UmlParameter> parameters;
}

class UmlLiteral {
  const UmlLiteral({
    required this.id,
    required this.elementId,
    required this.name,
    required this.orderIndex,
    this.value,
  });

  factory UmlLiteral.fromJson(Map<String, dynamic> json) {
    return UmlLiteral(
      id: (json['id'] ?? '').toString(),
      elementId: (json['elementId'] ?? '').toString(),
      name: (json['name'] ?? '').toString(),
      value: json['value'] as String?,
      orderIndex: json['orderIndex'] is num ? (json['orderIndex'] as num).toInt() : 0,
    );
  }

  final String id;
  final String elementId;
  final String name;
  final String? value;
  final int orderIndex;
}

class UmlRelationship {
  const UmlRelationship({
    required this.id,
    required this.diagramId,
    required this.sourceElementId,
    required this.targetElementId,
    required this.relationshipType,
    required this.direction,
    this.name,
    this.associationClassElementId,
    this.sourceHandle,
    this.targetHandle,
  });

  factory UmlRelationship.fromJson(Map<String, dynamic> json) {
    return UmlRelationship(
      id: (json['id'] ?? '').toString(),
      diagramId: (json['diagramId'] ?? '').toString(),
      sourceElementId: (json['sourceElementId'] ?? '').toString(),
      targetElementId: (json['targetElementId'] ?? '').toString(),
      relationshipType: (json['relationshipType'] ?? 'association').toString(),
      name: json['name'] as String?,
      direction: (json['direction'] ?? 'bidirectional').toString(),
      associationClassElementId: json['associationClassElementId'] as String?,
      sourceHandle: json['sourceHandle'] as String?,
      targetHandle: json['targetHandle'] as String?,
    );
  }

  final String id;
  final String diagramId;
  final String sourceElementId;
  final String targetElementId;
  final String relationshipType;
  final String? name;
  final String direction;
  final String? associationClassElementId;
  final String? sourceHandle;
  final String? targetHandle;
}

class UmlMultiplicity {
  const UmlMultiplicity({
    required this.id,
    required this.relationshipId,
    required this.end,
    required this.isOrdered,
    required this.isUnique,
    this.min,
    this.max,
  });

  factory UmlMultiplicity.fromJson(Map<String, dynamic> json) {
    return UmlMultiplicity(
      id: (json['id'] ?? '').toString(),
      relationshipId: (json['relationshipId'] ?? '').toString(),
      end: (json['end'] ?? 'source').toString(),
      min: json['min'] is num ? (json['min'] as num).toInt() : null,
      max: json['max'] is num ? (json['max'] as num).toInt() : null,
      isOrdered: json['isOrdered'] == true,
      isUnique: json['isUnique'] == true,
    );
  }

  final String id;
  final String relationshipId;
  final String end;
  final int? min;
  final int? max;
  final bool isOrdered;
  final bool isUnique;

  String get label {
    if (min == null && max == null) return '';
    final maxLabel = max == null ? '*' : (max! < 0 ? '*' : max.toString());
    if (min != null && max != null && min == max) return min.toString();
    return '${min ?? 0}..$maxLabel';
  }
}
