/// Evento de cambio recibido por STOMP. El backend envía `{ type, data }`.
class RealtimeChange {
  const RealtimeChange(this.type, this.data);

  final String type;
  final Map<String, dynamic> data;
}
