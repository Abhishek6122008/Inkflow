import 'dart:async';
import 'dart:convert';

import 'package:flutter_quill/quill_delta.dart' show Delta;
import 'package:stomp_dart_client/stomp.dart';
import 'package:stomp_dart_client/stomp_config.dart';
import 'package:stomp_dart_client/stomp_frame.dart';

import 'api_client.dart';

typedef RemoteOpHandler = void Function(Delta op, int version);
typedef VersionAckHandler = void Function(int version);
typedef SyncErrorHandler = void Function(String code, String message);
typedef PresenceHandler = void Function(List<PresenceUser> users);
typedef ConnectionStatusHandler = void Function(bool connected);

const _wsBaseUrl = 'http://localhost:8080';

class PresenceUser {
  const PresenceUser({required this.userId, required this.displayName});

  final String userId;
  final String displayName;

  factory PresenceUser.fromJson(Map<String, dynamic> json) =>
      PresenceUser(userId: json['userId'] as String, displayName: json['displayName'] as String);
}

/// Wraps a STOMP connection for one open document: authenticates with the
/// same JWT used for REST, subscribes to the document's broadcast topic,
/// presence topic, and this user's private error queue, and exposes
/// [sendOp] for local edits.
///
/// The server always echoes every applied op back on the topic, including
/// the sender's own (transformed) op — so the local edit is applied
/// immediately by flutter_quill when the user types, and the echo of that
/// same op is recognized by [authorId] match and skipped on [onRemoteOp];
/// only its version number is consumed via [onVersionAck].
///
/// `stomp_dart_client` auto-reconnects on drop (default 5s delay); [onConnectionStatus]
/// fires on both the initial connect and every reconnect/disconnect so the UI
/// can show live connection state rather than just "connected once".
class DocumentSyncService {
  DocumentSyncService({
    required this.documentId,
    required this.currentUserId,
    required ApiClient apiClient,
    required this.onRemoteOp,
    required this.onVersionAck,
    required this.onError,
    required this.onPresenceChanged,
    required this.onConnectionStatus,
  }) : _apiClient = apiClient;

  final String documentId;
  final String currentUserId;
  final ApiClient _apiClient;
  final RemoteOpHandler onRemoteOp;
  final VersionAckHandler onVersionAck;
  final SyncErrorHandler onError;
  final PresenceHandler onPresenceChanged;
  final ConnectionStatusHandler onConnectionStatus;

  StompClient? _client;

  Future<void> connect() async {
    final token = await _apiClient.readToken();
    final client = StompClient(
      config: StompConfig.SockJS(
        url: '$_wsBaseUrl/ws',
        stompConnectHeaders: token != null ? {'Authorization': 'Bearer $token'} : null,
        onConnect: _onConnect,
        onDisconnect: (_) => onConnectionStatus(false),
        onWebSocketDone: () => onConnectionStatus(false),
        onWebSocketError: (dynamic error) {
          onConnectionStatus(false);
          onError('CONNECTION_ERROR', error.toString());
        },
        onStompError: (frame) => onError('STOMP_ERROR', frame.body ?? 'Unknown STOMP error'),
      ),
    );
    _client = client;
    client.activate();
  }

  void _onConnect(StompFrame frame) {
    final client = _client;
    if (client == null) return;

    client.subscribe(
      destination: '/topic/doc/$documentId',
      callback: _handleBroadcast,
    );
    client.subscribe(
      destination: '/topic/doc/$documentId/presence',
      callback: _handlePresence,
    );
    client.subscribe(
      destination: '/user/queue/errors',
      callback: _handleErrorFrame,
    );
    onConnectionStatus(true);
  }

  void _handlePresence(StompFrame frame) {
    final body = frame.body;
    if (body == null) return;
    final json = jsonDecode(body) as Map<String, dynamic>;
    if (json['documentId'] != documentId) return;

    final users = (json['users'] as List)
        .map((u) => PresenceUser.fromJson(u as Map<String, dynamic>))
        .toList();
    onPresenceChanged(users);
  }

  void _handleBroadcast(StompFrame frame) {
    final body = frame.body;
    if (body == null) return;
    final json = jsonDecode(body) as Map<String, dynamic>;
    if (json['documentId'] != documentId) return;

    final version = json['version'] as int;
    final authorId = json['authorId'] as String;

    if (authorId == currentUserId) {
      onVersionAck(version);
      return;
    }

    final op = Delta.fromJson(json['op'] as List);
    onRemoteOp(op, version);
  }

  void _handleErrorFrame(StompFrame frame) {
    final body = frame.body;
    if (body == null) return;
    final json = jsonDecode(body) as Map<String, dynamic>;
    if (json['documentId'] != documentId) return;
    onError(json['code'] as String, json['message'] as String);
  }

  void sendOp(Delta op, int baseVersion) {
    final client = _client;
    if (client == null || !client.connected) return;

    client.send(
      destination: '/app/doc/$documentId/edit',
      body: jsonEncode({'op': op.toJson(), 'baseVersion': baseVersion}),
    );
  }

  void dispose() {
    _client?.deactivate();
    _client = null;
  }
}
