import '../entities/public_diagram_view.dart';
import '../entities/share_link_state.dart';

abstract interface class SharingRepository {
  Future<ShareLinkState> getStatus(String projectId, String diagramId);

  Future<ShareLinkState> enable(String projectId, String diagramId, String role);

  Future<void> disable(String projectId, String diagramId);

  Future<PublicDiagramView> publicView(String token);

  Future<ShareJoinResult> join(String token);
}
