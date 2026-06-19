import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:nes_ui/nes_ui.dart';

import '../api/api_client.dart';
import '../auth/auth_state.dart';

class DocumentScreen extends ConsumerStatefulWidget {
  const DocumentScreen({super.key});

  @override
  ConsumerState<DocumentScreen> createState() => _DocumentScreenState();
}

class _DocumentScreenState extends ConsumerState<DocumentScreen> {
  late final QuillController _controller;
  String? _documentId;
  String _title = 'My Document';
  bool _isLoading = true;
  bool _isSaving = false;
  String? _error;
  Timer? _saveDebounce;

  @override
  void initState() {
    super.initState();
    _controller = QuillController.basic();
    _loadOrCreateDocument();
  }

  @override
  void dispose() {
    _saveDebounce?.cancel();
    _controller.dispose();
    super.dispose();
  }

  Future<void> _loadOrCreateDocument() async {
    final api = ref.read(apiClientProvider);
    try {
      final docs = await api.listDocuments();
      Map<String, dynamic> summary;
      if (docs.isEmpty) {
        summary = await api.createDocument('My Document');
      } else {
        summary = docs.first as Map<String, dynamic>;
      }

      final document = await api.getDocument(summary['id'] as String);
      final delta = jsonDecode(document['content'] as String) as List;

      setState(() {
        _documentId = document['id'] as String;
        _title = document['title'] as String;
        _controller.document = Document.fromJson(delta);
        _isLoading = false;
      });

      _controller.addListener(_onDocumentChanged);
    } catch (e) {
      setState(() {
        _error = apiErrorMessage(e);
        _isLoading = false;
      });
    }
  }

  void _onDocumentChanged() {
    _saveDebounce?.cancel();
    _saveDebounce = Timer(const Duration(milliseconds: 800), _save);
  }

  Future<void> _save() async {
    if (_documentId == null) return;
    setState(() => _isSaving = true);
    try {
      final content = jsonEncode(_controller.document.toDelta().toJson());
      await ref.read(apiClientProvider).saveDocumentContent(_documentId!, content);
    } catch (e) {
      setState(() => _error = apiErrorMessage(e));
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  Future<void> _logout() async {
    await ref.read(authControllerProvider.notifier).logout();
    if (mounted) context.go('/login');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_title),
        actions: [
          if (_isSaving)
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16),
              child: Center(child: SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))),
            )
          else
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16),
              child: Center(child: Text('Saved')),
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
                    QuillSimpleToolbar(
                      configurations: QuillSimpleToolbarConfigurations(
                        controller: _controller,
                      ),
                    ),
                    const Divider(height: 1),
                    Expanded(
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: QuillEditor.basic(
                          configurations: QuillEditorConfigurations(
                            controller: _controller,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
    );
  }
}
