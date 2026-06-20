import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:nes_ui/nes_ui.dart';

import '../api/api_client.dart';
import '../auth/auth_state.dart';

class DocumentListScreen extends ConsumerStatefulWidget {
  const DocumentListScreen({super.key});

  @override
  ConsumerState<DocumentListScreen> createState() => _DocumentListScreenState();
}

class _DocumentListScreenState extends ConsumerState<DocumentListScreen> {
  List<Map<String, dynamic>> _documents = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadDocuments();
  }

  Future<void> _loadDocuments() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final docs = await ref.read(apiClientProvider).listDocuments();
      setState(() {
        _documents = docs.cast<Map<String, dynamic>>();
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = apiErrorMessage(e);
        _isLoading = false;
      });
    }
  }

  Future<void> _createDocument() async {
    try {
      final summary = await ref.read(apiClientProvider).createDocument('Untitled document');
      if (mounted) context.go('/documents/${summary['id']}');
    } catch (e) {
      setState(() => _error = apiErrorMessage(e));
    }
  }

  Future<void> _renameDocument(Map<String, dynamic> doc) async {
    final controller = TextEditingController(text: doc['title'] as String);
    final newTitle = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Rename document'),
        content: TextField(controller: controller, autofocus: true),
        actions: [
          TextButton(onPressed: () => context.pop(), child: const Text('Cancel')),
          TextButton(
            onPressed: () => context.pop(controller.text),
            child: const Text('Save'),
          ),
        ],
      ),
    );
    if (newTitle == null || newTitle.trim().isEmpty) return;
    try {
      await ref.read(apiClientProvider).renameDocument(doc['id'] as String, newTitle.trim());
      _loadDocuments();
    } catch (e) {
      setState(() => _error = apiErrorMessage(e));
    }
  }

  Future<void> _deleteDocument(Map<String, dynamic> doc) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete document'),
        content: Text('Delete "${doc['title']}"? This cannot be undone.'),
        actions: [
          TextButton(onPressed: () => context.pop(false), child: const Text('Cancel')),
          TextButton(onPressed: () => context.pop(true), child: const Text('Delete')),
        ],
      ),
    );
    if (confirmed != true) return;
    try {
      await ref.read(apiClientProvider).deleteDocument(doc['id'] as String);
      _loadDocuments();
    } catch (e) {
      setState(() => _error = apiErrorMessage(e));
    }
  }

  Future<void> _shareDocument(Map<String, dynamic> doc) async {
    final emailController = TextEditingController();
    String role = 'VIEWER';
    final result = await showDialog<bool>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text('Share document'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: emailController,
                autofocus: true,
                decoration: const InputDecoration(labelText: 'Collaborator email'),
              ),
              const SizedBox(height: 12),
              DropdownButton<String>(
                value: role,
                items: const [
                  DropdownMenuItem(value: 'VIEWER', child: Text('Viewer')),
                  DropdownMenuItem(value: 'EDITOR', child: Text('Editor')),
                ],
                onChanged: (value) => setDialogState(() => role = value ?? 'VIEWER'),
              ),
            ],
          ),
          actions: [
            TextButton(onPressed: () => context.pop(false), child: const Text('Cancel')),
            TextButton(onPressed: () => context.pop(true), child: const Text('Share')),
          ],
        ),
      ),
    );
    if (result != true || emailController.text.trim().isEmpty) return;
    try {
      await ref.read(apiClientProvider).addCollaborator(
            doc['id'] as String,
            emailController.text.trim(),
            role,
          );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Collaborator added')),
        );
      }
    } catch (e) {
      setState(() => _error = apiErrorMessage(e));
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
        title: const Text('My documents'),
        actions: [
          NesButton(
            type: NesButtonType.normal,
            onPressed: _logout,
            child: const Text('Log out'),
          ),
          const SizedBox(width: 12),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _createDocument,
        child: const Icon(Icons.add),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!, style: const TextStyle(color: Colors.red)))
              : _documents.isEmpty
                  ? const Center(child: Text('No documents yet — tap + to create one.'))
                  : ListView.builder(
                      itemCount: _documents.length,
                      itemBuilder: (context, index) {
                        final doc = _documents[index];
                        final role = doc['role'] as String?;
                        final isOwner = role == 'OWNER';
                        return ListTile(
                          title: Text(doc['title'] as String),
                          subtitle: Text(
                            'Updated ${doc['updatedAt']}'
                            '${role != null ? ' · $role' : ''}',
                          ),
                          onTap: () => context.go('/documents/${doc['id']}'),
                          trailing: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              if (isOwner) ...[
                                IconButton(
                                  icon: const Icon(Icons.person_add),
                                  onPressed: () => _shareDocument(doc),
                                ),
                                IconButton(
                                  icon: const Icon(Icons.edit),
                                  onPressed: () => _renameDocument(doc),
                                ),
                                IconButton(
                                  icon: const Icon(Icons.delete),
                                  onPressed: () => _deleteDocument(doc),
                                ),
                              ],
                            ],
                          ),
                        );
                      },
                    ),
    );
  }
}
