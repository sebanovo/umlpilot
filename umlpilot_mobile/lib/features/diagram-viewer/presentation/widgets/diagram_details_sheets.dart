import 'package:flutter/material.dart';

import '../../../../theme/app_colors.dart';
import '../../../../theme/app_spacing.dart';
import '../../../diagram-editor/domain/entities/uml_models.dart';

class ElementDetailsSheet extends StatelessWidget {
  const ElementDetailsSheet({
    super.key,
    required this.element,
    required this.attributes,
    required this.methods,
    required this.literals,
  });

  final UmlElement element;
  final List<UmlAttribute> attributes;
  final List<UmlMethod> methods;
  final List<UmlLiteral> literals;

  String _visibility(String value) {
    return switch (value) {
      'private' => 'privado',
      'protected' => 'protegido',
      'package' => 'paquete',
      _ => 'público',
    };
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(element.name, style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: AppSpacing.xs),
            Wrap(
              spacing: AppSpacing.sm,
              runSpacing: AppSpacing.xs,
              children: [
                _InfoChip(label: element.elementType),
                _InfoChip(label: _visibility(element.visibility)),
                if (element.stereotype != null && element.stereotype!.isNotEmpty)
                  _InfoChip(label: '«${element.stereotype}»'),
              ],
            ),
            if (element.elementType == 'note') ...[
              const SizedBox(height: AppSpacing.lg),
              Text(element.name, style: const TextStyle(color: AppColors.textSecondary)),
            ] else ...[
              const SizedBox(height: AppSpacing.lg),
              _SectionTitle(title: 'Atributos (${attributes.length})'),
              if (attributes.isEmpty)
                const _MutedText('Sin atributos')
              else
                ...attributes.map(
                  (attribute) => _DetailRow(
                    leading: _visibilitySymbol(attribute.visibility),
                    text: '${attribute.name}: ${attribute.dataType}',
                  ),
                ),
              const SizedBox(height: AppSpacing.lg),
              _SectionTitle(title: 'Métodos (${methods.length})'),
              if (methods.isEmpty)
                const _MutedText('Sin métodos')
              else
                ...methods.map(
                  (method) => _DetailRow(
                    leading: _visibilitySymbol(method.visibility),
                    text: '${method.name}(${method.parameters.map((p) => '${p.name}: ${p.dataType}').join(', ')}): ${method.returnType}',
                  ),
                ),
              if (element.elementType == 'enumeration') ...[
                const SizedBox(height: AppSpacing.lg),
                _SectionTitle(title: 'Literales (${literals.length})'),
                if (literals.isEmpty)
                  const _MutedText('Sin literales')
                else
                  ...literals.map((literal) => _DetailRow(leading: '•', text: literal.name)),
              ],
            ],
          ],
        ),
      ),
    );
  }

  String _visibilitySymbol(String value) {
    return switch (value) {
      'private' => '-',
      'protected' => '#',
      'package' => '~',
      _ => '+',
    };
  }
}

class RelationshipDetailsSheet extends StatelessWidget {
  const RelationshipDetailsSheet({
    super.key,
    required this.relationship,
    required this.multiplicities,
    required this.sourceName,
    required this.targetName,
  });

  final UmlRelationship relationship;
  final List<UmlMultiplicity> multiplicities;
  final String sourceName;
  final String targetName;

  @override
  Widget build(BuildContext context) {
    final source = multiplicities.where((item) => item.end == 'source').firstOrNull;
    final target = multiplicities.where((item) => item.end == 'target').firstOrNull;
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(relationship.name?.isNotEmpty == true ? relationship.name! : 'Relación',
                style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: AppSpacing.xs),
            Wrap(
              spacing: AppSpacing.sm,
              runSpacing: AppSpacing.xs,
              children: [
                _InfoChip(label: relationship.relationshipType),
                _InfoChip(label: relationship.direction),
              ],
            ),
            const SizedBox(height: AppSpacing.lg),
            Row(
              children: [
                Expanded(child: _Endpoint(title: 'Origen', name: sourceName, multiplicity: source?.label ?? '')),
                const Padding(
                  padding: EdgeInsets.symmetric(horizontal: AppSpacing.sm),
                  child: Icon(Icons.arrow_forward, size: 18, color: AppColors.textMuted),
                ),
                Expanded(child: _Endpoint(title: 'Destino', name: targetName, multiplicity: target?.label ?? '')),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _Endpoint extends StatelessWidget {
  const _Endpoint({required this.title, required this.name, required this.multiplicity});

  final String title;
  final String name;
  final String multiplicity;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: const TextStyle(color: AppColors.textMuted, fontSize: 11)),
        const SizedBox(height: 2),
        Text(name, style: const TextStyle(color: AppColors.textPrimary, fontWeight: FontWeight.w600)),
        if (multiplicity.isNotEmpty)
          Text(multiplicity, style: const TextStyle(color: AppColors.primaryLight, fontSize: 12)),
      ],
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle({required this.title});

  final String title;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.sm),
      child: Text(title, style: Theme.of(context).textTheme.titleMedium),
    );
  }
}

class _MutedText extends StatelessWidget {
  const _MutedText(this.text);

  final String text;

  @override
  Widget build(BuildContext context) {
    return Text(text, style: const TextStyle(color: AppColors.textMuted, fontSize: 13));
  }
}

class _DetailRow extends StatelessWidget {
  const _DetailRow({required this.leading, required this.text});

  final String leading;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 18,
            child: Text(leading, style: const TextStyle(fontFamily: 'monospace', color: AppColors.primaryLight)),
          ),
          Expanded(
            child: Text(
              text,
              style: const TextStyle(fontFamily: 'monospace', fontSize: 12, color: AppColors.textPrimary),
            ),
          ),
        ],
      ),
    );
  }
}

class _InfoChip extends StatelessWidget {
  const _InfoChip({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm, vertical: 2),
      decoration: BoxDecoration(
        color: AppColors.surfaceAlt,
        borderRadius: BorderRadius.circular(AppRadius.full),
        border: Border.all(color: AppColors.border),
      ),
      child: Text(label, style: const TextStyle(color: AppColors.textSecondary, fontSize: 11)),
    );
  }
}

