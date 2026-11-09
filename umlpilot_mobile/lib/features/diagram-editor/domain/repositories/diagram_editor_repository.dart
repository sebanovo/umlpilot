import '../entities/uml_models.dart';

abstract interface class DiagramEditorRepository {
  Future<List<UmlElement>> listElements(String projectId, String diagramId);

  Future<UmlElement> createElement(
    String projectId,
    String diagramId, {
    required String name,
    required String elementType,
    required int positionX,
    required int positionY,
  });

  Future<UmlElement> updateElement(
    String projectId,
    String diagramId,
    String elementId, {
    String? name,
    String? visibility,
    String? stereotype,
    int? positionX,
    int? positionY,
    int? width,
    int? height,
  });

  Future<void> deleteElement(String projectId, String diagramId, String elementId);

  Future<void> deleteAllElements(String projectId, String diagramId);

  Future<List<UmlAttribute>> listAttributes(String projectId, String diagramId, String elementId);

  Future<UmlAttribute> addAttribute(
    String projectId,
    String diagramId,
    String elementId, {
    required String name,
    required String dataType,
    String? visibility,
    int? orderIndex,
  });

  Future<void> deleteAttribute(String projectId, String diagramId, String attributeId);

  Future<List<UmlMethod>> listMethods(String projectId, String diagramId, String elementId);

  Future<UmlMethod> addMethod(
    String projectId,
    String diagramId,
    String elementId, {
    required String name,
    required String returnType,
    String? visibility,
    int? orderIndex,
  });

  Future<void> deleteMethod(String projectId, String diagramId, String methodId);

  Future<UmlParameter> addParameter(
    String projectId,
    String diagramId,
    String methodId, {
    required String name,
    required String dataType,
    String? direction,
    int? orderIndex,
  });

  Future<void> deleteParameter(String projectId, String diagramId, String parameterId);

  Future<List<UmlLiteral>> listLiterals(String projectId, String diagramId, String elementId);

  Future<UmlLiteral> addLiteral(
    String projectId,
    String diagramId,
    String elementId, {
    required String name,
    String? value,
    int? orderIndex,
  });

  Future<void> deleteLiteral(String projectId, String diagramId, String literalId);

  Future<List<UmlRelationship>> listRelationships(String projectId, String diagramId);

  Future<UmlRelationship> createRelationship(
    String projectId,
    String diagramId, {
    required String sourceElementId,
    required String targetElementId,
    required String relationshipType,
  });

  Future<UmlRelationship> updateRelationship(
    String projectId,
    String diagramId,
    String relationshipId, {
    String? name,
    String? type,
    String? direction,
  });

  Future<void> deleteRelationship(String projectId, String diagramId, String relationshipId);

  Future<List<UmlMultiplicity>> listMultiplicities(
    String projectId,
    String diagramId,
    String relationshipId,
  );

  Future<UmlMultiplicity> updateMultiplicity(
    String projectId,
    String diagramId,
    String multiplicityId, {
    int? min,
    int? max,
  });
}
