import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'package:flutter_quill/quill_delta.dart' show Delta;
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:nes_ui/nes_ui.dart';

import '../api/api_client.dart';
import '../api/document_sync_service.dart';
import '../auth/auth_state.dart';

class DocumentScreen extends ConsumerStatefulWidget {
  const DocumentScreen({super.key, required this.documentId});

  final String documentId;

  @override
  ConsumerState<DocumentScreen> createState() => _DocumentScreenState();
}

class _DocumentScreenState extends ConsumerState<DocumentScreen> {
  late final QuillController _controller;
  String _title = '';
  String? _role;
  bool _isLoading = true;
  bool _isConnected = false;
  String? _error;
  int _version = 0;
  DocumentSyncService? _sync;
  StreamSubscription<DocChange>? _changeSubscription;
  List<PresenceUser> _presentUsers = const [];

  bool get _canEdit => _role != 'VIEWER';

  @override
  void initState() {
    super.initState();
    _controller = QuillController.basic();
    _loadDocument();
  }

  @override
  void dispose() {
    _changeSubscription?.cancel();
    _sync?.dispose();
    _controller.dispose();
    super.dispose();
  }

  Future<void> _loadDocument() async {
    final api = ref.read(apiClientProvider);
    try {
      final document = await api.getDocument(widget.documentId);
      final delta = jsonDecode(document['content'] as String) as List;

      setState(() {
        _title = document['title'] as String;
        _role = document['role'] as String?;
        _version = document['version'] as int;
        _controller.document = Document.fromJson(delta);
        _isLoading = false;
      });

      if (_canEdit) {
        _changeSubscription = _controller.document.changes.listen(_onDocumentChange);
      }
      await _connectSync();
    } catch (e) {
      setState(() {
        _error = apiErrorMessage(e);
        _isLoading = false;
      });
    }
  }

  Future<void> _connectSync() async {
    final userId = ref.read(authControllerProvider).user?.id;
    if (userId == null) return;

    final sync = DocumentSyncService(
      documentId: widget.documentId,
      currentUserId: userId,
      apiClient: ref.read(apiClientProvider),
      onRemoteOp: _applyRemoteOp,
      onVersionAck: (version) => setState(() => _version = version),
      onError: (code, message) => setState(() => _error = '$code: $message'),
      onPresenceChanged: (users) => setState(() => _presentUsers = users),
      onConnectionStatus: (connected) => setState(() => _isConnected = connected),
    );
    _sync = sync;
    await sync.connect();
  }

  void _onDocumentChange(DocChange change) {
    if (change.source != ChangeSource.local) return;
    _sync?.sendOp(change.change, _version);
  }

  void _applyRemoteOp(Delta op, int version) {
    _controller.compose(op, _controller.selection, ChangeSource.remote);
    setState(() => _version = version);
  }

  Future<void> _logout() async {
    await ref.read(authControllerProvider.notifier).logout();
    if (mounted) context.go('/login');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.go('/documents'),
        ),
        title: Text(_title),
        actions: [
          if (_presentUsers.isNotEmpty) _PresenceRow(users: _presentUsers),
          if (!_isConnected)
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16),
              child: Center(child: SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))),
            )
          else if (!_canEdit)
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16),
              child: Center(child: Text('View only')),
            )
          else
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16),
              child: Center(child: Text('Live')),
            ),
          NesButton(
            type: NesButtonType.normal,
            onPressed: _logout,
            child: const Text('Log out'),
          ),
          const SizedBox(width: 12),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!, style: const TextStyle(color: Colors.red)))
              : Column(
                  children: [
                    if (_canEdit) ...[
                      QuillSimpleToolbar(
                        configurations: QuillSimpleToolbarConfigurations(
                          controller: _controller,
                        ),
                      ),
                      const Divider(height: 1),
                    ],
                    Expanded(
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: QuillEditor.basic(
                          configurations: QuillEditorConfigurations(
                            controller: _controller,
                            readOnly: !_canEdit,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
    );
  }
}

class _PresenceRow extends StatelessWidget {
  const _PresenceRow({required this.users});

  final List<PresenceUser> users;

  @override
  Widget build(BuildContext context) {
    final shown = users.take(5).toList();
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8),
      child: Tooltip(
        message: users.map((u) => u.displayName).join(', '),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            for (final user in shown)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 2),
                child: CircleAvatar(
                  radius: 14,
                  child: Text(
                    user.displayName.isNotEmpty ? user.displayName[0].toUpperCase() : '?',
                    style: const TextStyle(fontSize: 12),
                  ),
                ),
              ),
            if (users.length > shown.length)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 2),
                child: CircleAvatar(
                  radius: 14,
                  child: Text('+${users.length - shown.length}', style: const TextStyle(fontSize: 11)),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
