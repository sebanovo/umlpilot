import '../../../diagram-editor/domain/entities/uml_models.dart';

class PublicDiagramView {
  const PublicDiagramView({
    required this.projectId,
    required this.diagramId,
    required this.linkRole,
    required this.projectName,
    required this.diagramName,
    required this.diagramType,
    required this.elements,
    required this.relationships,
    required this.attributes,
    required this.methods,
    required this.literals,
    required this.multiplicities,
  });

  factory PublicDiagramView.fromJson(Map<String, dynamic> json) {
    List<T> parseList<T>(dynamic raw, T Function(Map<String, dynamic>) parse) {
      if (raw is! List) return const [];
      return raw.map((item) => parse((item as Map).cast<String, dynamic>())).toList();
    }

    Map<String, List<T>> parseMap<T>(dynamic raw, T Function(Map<String, dynamic>) parse) {
      final result = <String, List<T>>{};
      if (raw is Map) {
        raw.forEach((key, value) {
          result[key.toString()] = parseList<T>(value, parse);
        });
      }
      return result;
    }

    return PublicDiagramView(
      projectId: (json['projectId'] ?? '').toString(),
      diagramId: (json['diagramId'] ?? '').toString(),
      linkRole: (json['linkRole'] ?? 'viewer').toString(),
      projectName: (json['projectName'] ?? '').toString(),
      diagramName: (json['diagramName'] ?? '').toString(),
      diagramType: (json['diagramType'] ?? 'class').toString(),
      elements: parseList(json['elements'], UmlElement.fromJson),
      relationships: parseList(json['relationships'], UmlRelationship.fromJson),
      attributes: parseMap(json['attributes'], UmlAttribute.fromJson),
      methods: parseMap(json['methods'], UmlMethod.fromJson),
      literals: parseMap(json['literals'], UmlLiteral.fromJson),
      multiplicities: parseMap(json['multiplicities'], UmlMultiplicity.fromJson),
    );
  }

  final String projectId;
  final String diagramId;
  final String linkRole;
  final String projectName;
  final String diagramName;
  final String diagramType;
  final List<UmlElement> elements;
  final List<UmlRelationship> relationships;
  final Map<String, List<UmlAttribute>> attributes;
  final Map<String, List<UmlMethod>> methods;
  final Map<String, List<UmlLiteral>> literals;
  final Map<String, List<UmlMultiplicity>> multiplicities;
}
