package com.umlpilot.umlpilot_api.application.generation;

import com.umlpilot.umlpilot_api.application.dto.DiagramSnapshot;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.infrastructure.config.CodeGenerationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Construye el {@link BackendModel} a partir de una {@link DiagramSnapshot}: aplica el
 * tratamiento de elementos, el mapeo de tipos, la resolución de relaciones a JPA y
 * precalcula los bloques de texto que consumen las plantillas.
 */
@Component
public class BackendModelBuilder {

    private final CodeGenerationProperties properties;

    public BackendModelBuilder(CodeGenerationProperties properties) {
        this.properties = properties;
    }

    public BackendModel build(DiagramSnapshot snapshot) {
        List<DiagramSnapshot.ElementSnapshot> entityElements = snapshot.elements().stream()
                .filter(BackendModelBuilder::isEntityElement)
                .toList();

        if (entityElements.isEmpty()) {
            throw new DomainException("El diagrama no contiene clases para generar el backend");
        }
        if (entityElements.size() > properties.getMaxClasses()) {
            throw new DomainException("El diagrama supera el máximo de clases permitido ("
                    + properties.getMaxClasses() + ")");
        }

        String basePackage = (properties.getBasePackage() == null || properties.getBasePackage().isBlank())
                ? "com.generated"
                : properties.getBasePackage().trim();

        Map<String, EntityBuilder> byElementId = new LinkedHashMap<>();
        Set<String> usedClassNames = new HashSet<>();
        for (DiagramSnapshot.ElementSnapshot element : entityElements) {
            String className = unique(NameUtils.className(element.name()), usedClassNames, 2);
            usedClassNames.add(className);
            EntityBuilder builder = new EntityBuilder(element, className, basePackage);
            builder.resolveIdAndFields();
            byElementId.put(element.id(), builder);
        }

        for (EntityBuilder builder : byElementId.values()) {
            builder.resolveMethods();
        }

        for (DiagramSnapshot.RelationshipSnapshot relationship : snapshot.relationships()) {
            applyRelationship(relationship, byElementId);
        }

        List<BackendModel.Entity> entities = byElementId.values().stream()
                .map(EntityBuilder::toEntity)
                .toList();
        List<String> entityList = entities.stream()
                .map(e -> "- `" + e.className() + "` -> tabla `" + e.tableName()
                        + "`, CRUD en `/api/v1/" + e.resourcePath() + "`")
                .toList();

        return new BackendModel(basePackage, basePackage.replace('.', '/'), "generated-backend",
                entityList, entities);
    }

    /** Las notas se ignoran; cualquier otro tipo (incluido uno desconocido) se trata como entidad. */
    private static boolean isEntityElement(DiagramSnapshot.ElementSnapshot element) {
        String type = element.elementType();
        if (type == null || type.isBlank()) {
            return true;
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        return !normalized.equals("note") && !normalized.equals("nota");
    }

    private void applyRelationship(DiagramSnapshot.RelationshipSnapshot relationship,
                                   Map<String, EntityBuilder> byElementId) {
        EntityBuilder source = byElementId.get(relationship.sourceElementId());
        EntityBuilder target = byElementId.get(relationship.targetElementId());
        if (source == null || target == null) {
            return;
        }

        String type = (relationship.type() == null || relationship.type().isBlank())
                ? "association"
                : relationship.type().trim().toLowerCase(Locale.ROOT);
        EntityBuilder associationClass = (relationship.associationClassElementId() == null)
                ? null
                : byElementId.get(relationship.associationClassElementId());

        // La clase asociación es la entidad enlace del muchos-a-muchos: tiene las dos claves
        // foráneas (@ManyToOne a cada extremo) en lugar de una tabla intermedia aparte.
        if ("association_class".equals(type) && associationClass != null
                && associationClass != source && associationClass != target) {
            associationClass.addManyToOne(source, true);
            associationClass.addManyToOne(target, true);
            return;
        }

        boolean sourceMany = isMany(relationship.sourceMultiplicity());
        boolean targetMany = isMany(relationship.targetMultiplicity());

        if (sourceMany && targetMany) {
            source.addManyToMany(target);
        } else if (targetMany) {
            target.addManyToOne(source, minOf(relationship.sourceMultiplicity()) >= 1);
        } else if (sourceMany) {
            source.addManyToOne(target, minOf(relationship.targetMultiplicity()) >= 1);
        } else {
            source.addOneToOne(target, minOf(relationship.targetMultiplicity()) >= 1);
        }
    }

    static boolean isMany(String label) {
        if (label == null || label.isBlank()) {
            return false;
        }
        String value = label.trim();
        if (value.equals("*")) {
            return true;
        }
        if (value.contains("..")) {
            String upper = value.substring(value.indexOf("..") + 2).trim();
            if (upper.equals("*")) {
                return true;
            }
            return parseInt(upper, 1) > 1;
        }
        return parseInt(value, 1) > 1;
    }

    static int minOf(String label) {
        if (label == null || label.isBlank()) {
            return 1;
        }
        String value = label.trim();
        if (value.equals("*")) {
            return 0;
        }
        if (value.contains("..")) {
            return parseInt(value.substring(0, value.indexOf("..")), 0);
        }
        return parseInt(value, 1);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static String unique(String base, Set<String> used, int start) {
        String candidate = (base == null || base.isBlank()) ? "item" : base;
        if (!used.contains(candidate)) {
            return candidate;
        }
        int index = start;
        while (used.contains(candidate + index)) {
            index++;
        }
        return candidate + index;
    }

    private static final class EntityBuilder {
        private final DiagramSnapshot.ElementSnapshot element;
        private final String className;
        private final String varName;
        private final String tableName;
        private final String basePackage;
        private final List<BackendModel.Field> fields = new ArrayList<>();
        private final List<BackendModel.Reference> references = new ArrayList<>();
        private final List<BackendModel.Method> methods = new ArrayList<>();
        private final Set<String> usedFieldNames = new HashSet<>();
        private final Set<String> typeImports = new LinkedHashSet<>();
        private String idFieldName = "id";
        private String idType = "Long";

        EntityBuilder(DiagramSnapshot.ElementSnapshot element, String className, String basePackage) {
            this.element = element;
            this.className = className;
            this.basePackage = basePackage;
            this.varName = NameUtils.camelCase(className);
            this.tableName = NameUtils.pluralize(NameUtils.snakeCase(className));
        }

        void resolveIdAndFields() {
            List<DiagramSnapshot.AttributeSnapshot> attributes =
                    element.attributes() == null ? List.of() : element.attributes();

            DiagramSnapshot.AttributeSnapshot idAttribute = null;
            for (DiagramSnapshot.AttributeSnapshot attribute : attributes) {
                if (attribute.name() != null && attribute.name().trim().equalsIgnoreCase("id")) {
                    idAttribute = attribute;
                    break;
                }
            }

            if (idAttribute != null) {
                idFieldName = NameUtils.fieldName(idAttribute.name());
                JavaTypeMapper.JavaType idJavaType = JavaTypeMapper.map(idAttribute.dataType());
                idType = idJavaType.name();
                if (idJavaType.needsImport()) {
                    typeImports.add(idJavaType.importFqcn());
                }
                usedFieldNames.add(idFieldName);
            } else {
                usedFieldNames.add("id");
            }

            for (DiagramSnapshot.AttributeSnapshot attribute : attributes) {
                if (attribute == idAttribute || attribute.name() == null || attribute.name().isBlank()) {
                    continue;
                }
                String fieldName = unique(NameUtils.fieldName(attribute.name()), usedFieldNames, 2);
                usedFieldNames.add(fieldName);
                JavaTypeMapper.JavaType type = JavaTypeMapper.map(attribute.dataType());
                if (type.needsImport()) {
                    typeImports.add(type.importFqcn());
                }
                fields.add(new BackendModel.Field(fieldName, type.name(),
                        NameUtils.snakeCase(attribute.name()),
                        type.needsImport() ? type.importFqcn() : null,
                        NameUtils.getterName(fieldName), NameUtils.setterName(fieldName)));
            }
        }

        void resolveMethods() {
            List<DiagramSnapshot.MethodSnapshot> methodSnapshots =
                    element.methods() == null ? List.of() : element.methods();
            Set<String> reserved = accessorNames();
            Set<String> signatures = new HashSet<>();

            for (DiagramSnapshot.MethodSnapshot method : methodSnapshots) {
                if (method.name() == null || method.name().isBlank() || isConstructorName(method.name())) {
                    continue;
                }
                String name = NameUtils.fieldName(method.name());
                if (reserved.contains(name)) {
                    continue;
                }

                List<DiagramSnapshot.ParameterSnapshot> parameters =
                        method.parameters() == null ? List.of() : method.parameters();
                StringBuilder signature = new StringBuilder();
                Set<String> usedParamNames = new HashSet<>();
                int count = 0;
                for (DiagramSnapshot.ParameterSnapshot parameter : parameters) {
                    if (parameter.name() == null || parameter.name().isBlank()) {
                        continue;
                    }
                    JavaTypeMapper.JavaType paramType = JavaTypeMapper.map(parameter.dataType());
                    if (paramType.needsImport()) {
                        typeImports.add(paramType.importFqcn());
                    }
                    String paramName = unique(NameUtils.fieldName(parameter.name()), usedParamNames, 2);
                    usedParamNames.add(paramName);
                    if (signature.length() > 0) {
                        signature.append(", ");
                    }
                    signature.append(paramType.name()).append(' ').append(paramName);
                    count++;
                }

                if (!signatures.add(name + "/" + count)) {
                    continue;
                }

                String returnType = method.returnType();
                boolean returnsVoid = returnType == null || returnType.isBlank()
                        || returnType.trim().equalsIgnoreCase("void");
                JavaTypeMapper.JavaType mapped = returnsVoid ? null : JavaTypeMapper.map(returnType);
                if (mapped != null && mapped.needsImport()) {
                    typeImports.add(mapped.importFqcn());
                }
                methods.add(new BackendModel.Method(name, returnsVoid ? "void" : mapped.name(),
                        signature.toString(), returnsVoid));
            }
        }

        void addManyToOne(EntityBuilder target, boolean required) {
            String fieldName = unique(NameUtils.camelCase(target.className), usedFieldNames, 2);
            usedFieldNames.add(fieldName);
            references.add(new BackendModel.Reference(
                    fieldName, NameUtils.getterName(fieldName), NameUtils.setterName(fieldName),
                    "MANY_TO_ONE", true, false, false, false, required,
                    target.className, target.varName, target.idType,
                    NameUtils.snakeCase(target.className) + "_id",
                    null, null, null,
                    fieldName + "Id"));
        }

        void addOneToOne(EntityBuilder target, boolean required) {
            String fieldName = unique(NameUtils.camelCase(target.className), usedFieldNames, 2);
            usedFieldNames.add(fieldName);
            references.add(new BackendModel.Reference(
                    fieldName, NameUtils.getterName(fieldName), NameUtils.setterName(fieldName),
                    "ONE_TO_ONE", false, true, false, false, required,
                    target.className, target.varName, target.idType,
                    NameUtils.snakeCase(target.className) + "_id",
                    null, null, null,
                    fieldName + "Id"));
        }

        void addManyToMany(EntityBuilder target) {
            String pluralTarget = NameUtils.camelCase(NameUtils.pluralize(NameUtils.snakeCase(target.className)));
            String fieldName = unique(pluralTarget, usedFieldNames, 2);
            usedFieldNames.add(fieldName);
            String joinTable = NameUtils.pluralize(NameUtils.snakeCase(className)) + "_"
                    + NameUtils.pluralize(NameUtils.snakeCase(target.className));
            references.add(new BackendModel.Reference(
                    fieldName, NameUtils.getterName(fieldName), NameUtils.setterName(fieldName),
                    "MANY_TO_MANY", false, false, true, true, false,
                    target.className, target.varName, target.idType,
                    null, joinTable,
                    NameUtils.snakeCase(className) + "_id", NameUtils.snakeCase(target.className) + "_id",
                    fieldName + "Ids"));
        }

        private Set<String> accessorNames() {
            Set<String> names = new HashSet<>();
            names.add(NameUtils.getterName(idFieldName));
            names.add(NameUtils.setterName(idFieldName));
            for (BackendModel.Field field : fields) {
                names.add(NameUtils.getterName(field.name()));
                names.add(NameUtils.setterName(field.name()));
            }
            return names;
        }

        private boolean isConstructorName(String name) {
            String trimmed = name.trim();
            return trimmed.equalsIgnoreCase(className)
                    || (element.name() != null && trimmed.equalsIgnoreCase(element.name().trim()));
        }

        BackendModel.Entity toEntity() {
            List<BackendModel.DtoField> dtoFields = buildDtoFields();

            List<String> importStatements = new ArrayList<>();
            for (String fqcn : typeImports) {
                importStatements.add("import " + fqcn + ";");
            }
            if (references.stream().anyMatch(BackendModel.Reference::collection)) {
                importStatements.add("import java.util.List;");
            }
            importStatements.sort(String::compareTo);

            List<String> dtoImportStatements = new ArrayList<>(importStatements);
            for (BackendModel.DtoField dtoField : dtoFields) {
                if (dtoField.validationImport() != null) {
                    dtoImportStatements.add("import " + dtoField.validationImport() + ";");
                }
            }
            dtoImportStatements = dtoImportStatements.stream().distinct().sorted().toList();

            return new BackendModel.Entity(
                    className, varName, tableName, tableName, idFieldName, idType,
                    NameUtils.getterName(idFieldName), NameUtils.setterName(idFieldName),
                    List.copyOf(fields), List.copyOf(references), List.copyOf(methods), List.copyOf(dtoFields),
                    importStatements,
                    dtoImportStatements,
                    fieldDeclarations(),
                    referenceDeclarations(),
                    accessorDeclarations(),
                    methodDeclarations(),
                    dtoRequestComponents(dtoFields),
                    dtoResponseComponents(dtoFields),
                    serviceReferencedImports(),
                    serviceFields(),
                    constructorParameters(),
                    constructorAssignments(),
                    applyStatements(),
                    responseArguments());
        }

        private List<BackendModel.DtoField> buildDtoFields() {
            List<BackendModel.DtoField> dtoFields = new ArrayList<>();
            for (BackendModel.Field field : fields) {
                boolean notBlank = "String".equals(field.javaType());
                dtoFields.add(new BackendModel.DtoField(
                        field.name(), field.javaType(), field.importFqcn(),
                        notBlank ? "@NotBlank" : null,
                        notBlank ? "jakarta.validation.constraints.NotBlank" : null));
            }
            for (BackendModel.Reference reference : references) {
                if (reference.collection()) {
                    dtoFields.add(new BackendModel.DtoField(
                            reference.dtoFieldName(), "List<" + reference.targetIdType() + ">",
                            "java.util.List",
                            reference.required() ? "@NotNull" : null,
                            reference.required() ? "jakarta.validation.constraints.NotNull" : null));
                } else {
                    dtoFields.add(new BackendModel.DtoField(
                            reference.dtoFieldName(), reference.targetIdType(), null,
                            reference.required() ? "@NotNull" : null,
                            reference.required() ? "jakarta.validation.constraints.NotNull" : null));
                }
            }
            return dtoFields;
        }

        private List<String> fieldDeclarations() {
            List<String> declarations = new ArrayList<>();
            for (BackendModel.Field field : fields) {
                declarations.add("    @Column(name = \"" + field.columnName() + "\")\n"
                        + "    private " + field.javaType() + " " + field.name() + ";");
            }
            return declarations;
        }

        private List<String> referenceDeclarations() {
            List<String> declarations = new ArrayList<>();
            for (BackendModel.Reference reference : references) {
                StringBuilder block = new StringBuilder();
                if (reference.manyToOne()) {
                    block.append("    @ManyToOne(fetch = FetchType.LAZY)\n")
                            .append("    @JoinColumn(name = \"").append(reference.joinColumnName()).append("\")");
                } else if (reference.oneToOne()) {
                    block.append("    @OneToOne(fetch = FetchType.LAZY)\n")
                            .append("    @JoinColumn(name = \"").append(reference.joinColumnName()).append("\")");
                } else {
                    block.append("    @ManyToMany(fetch = FetchType.LAZY)\n")
                            .append("    @JoinTable(name = \"").append(reference.joinTableName()).append("\",\n")
                            .append("            joinColumns = @JoinColumn(name = \"")
                            .append(reference.joinTableJoinColumn()).append("\"),\n")
                            .append("            inverseJoinColumns = @JoinColumn(name = \"")
                            .append(reference.joinTableInverseColumn()).append("\"))");
                }
                block.append("\n    private ");
                if (reference.collection()) {
                    block.append("List<").append(reference.targetClassName()).append(">");
                } else {
                    block.append(reference.targetClassName());
                }
                block.append(' ').append(reference.fieldName()).append(';');
                declarations.add(block.toString());
            }
            return declarations;
        }

        private List<String> accessorDeclarations() {
            List<String> declarations = new ArrayList<>();
            declarations.add("    public " + idType + " " + NameUtils.getterName(idFieldName)
                    + "() { return " + idFieldName + "; }");
            declarations.add("    public void " + NameUtils.setterName(idFieldName) + "(" + idType + " "
                    + idFieldName + ") { this." + idFieldName + " = " + idFieldName + "; }");
            for (BackendModel.Field field : fields) {
                declarations.add("    public " + field.javaType() + " " + field.getterName()
                        + "() { return " + field.name() + "; }");
                declarations.add("    public void " + field.setterName() + "(" + field.javaType() + " "
                        + field.name() + ") { this." + field.name() + " = " + field.name() + "; }");
            }
            for (BackendModel.Reference reference : references) {
                String type = reference.collection()
                        ? "List<" + reference.targetClassName() + ">"
                        : reference.targetClassName();
                declarations.add("    public " + type + " " + reference.getterName()
                        + "() { return " + reference.fieldName() + "; }");
                declarations.add("    public void " + reference.setterName() + "(" + type + " "
                        + reference.fieldName() + ") { this." + reference.fieldName() + " = "
                        + reference.fieldName() + "; }");
            }
            return declarations;
        }

        private List<String> methodDeclarations() {
            List<String> declarations = new ArrayList<>();
            for (BackendModel.Method method : methods) {
                if (method.returnsVoid()) {
                    declarations.add("    public void " + method.name() + "(" + method.parameters() + ") {\n    }");
                } else {
                    declarations.add("    public " + method.returnType() + " " + method.name()
                            + "(" + method.parameters() + ") {\n"
                            + "        throw new UnsupportedOperationException(\"Metodo no implementado\");\n    }");
                }
            }
            return declarations;
        }

        private List<String> dtoRequestComponents(List<BackendModel.DtoField> dtoFields) {
            List<String> components = new ArrayList<>();
            for (BackendModel.DtoField dtoField : dtoFields) {
                String prefix = dtoField.validation() == null ? "" : dtoField.validation() + " ";
                components.add(prefix + dtoField.javaType() + " " + dtoField.name());
            }
            return components;
        }

        private List<String> dtoResponseComponents(List<BackendModel.DtoField> dtoFields) {
            List<String> components = new ArrayList<>();
            for (BackendModel.DtoField dtoField : dtoFields) {
                components.add(", " + dtoField.javaType() + " " + dtoField.name());
            }
            return components;
        }

        private List<String> serviceReferencedImports() {
            List<String> imports = new ArrayList<>();
            for (BackendModel.Reference reference : references) {
                imports.add("import " + basePackage + ".model." + reference.targetClassName() + ";");
                imports.add("import " + basePackage + ".repository." + reference.targetClassName() + "Repository;");
            }
            return imports;
        }

        private List<String> serviceFields() {
            List<String> fieldsBlock = new ArrayList<>();
            for (BackendModel.Reference reference : references) {
                fieldsBlock.add("    private final " + reference.targetClassName() + "Repository "
                        + reference.targetVarName() + "Repository;");
            }
            return fieldsBlock;
        }

        private String constructorParameters() {
            StringBuilder parameters = new StringBuilder(className + "Repository " + varName + "Repository");
            for (BackendModel.Reference reference : references) {
                parameters.append(", ").append(reference.targetClassName()).append("Repository ")
                        .append(reference.targetVarName()).append("Repository");
            }
            return parameters.toString();
        }

        private List<String> constructorAssignments() {
            List<String> assignments = new ArrayList<>();
            assignments.add("        this." + varName + "Repository = " + varName + "Repository;");
            for (BackendModel.Reference reference : references) {
                assignments.add("        this." + reference.targetVarName() + "Repository = "
                        + reference.targetVarName() + "Repository;");
            }
            return assignments;
        }

        private List<String> applyStatements() {
            List<String> statements = new ArrayList<>();
            for (BackendModel.Field field : fields) {
                statements.add("        entity." + field.setterName() + "(request." + field.name() + "());");
            }
            for (BackendModel.Reference reference : references) {
                if (reference.collection()) {
                    statements.add("        if (request." + reference.dtoFieldName() + "() == null) {\n"
                            + "            entity." + reference.setterName() + "(List.of());\n"
                            + "        } else {\n"
                            + "            entity." + reference.setterName() + "(" + reference.targetVarName()
                            + "Repository.findAllById(request." + reference.dtoFieldName() + "()));\n"
                            + "        }");
                } else {
                    statements.add("        if (request." + reference.dtoFieldName() + "() == null) {\n"
                            + "            entity." + reference.setterName() + "(null);\n"
                            + "        } else {\n"
                            + "            entity." + reference.setterName() + "(" + reference.targetVarName()
                            + "Repository.findById(request." + reference.dtoFieldName() + "())\n"
                            + "                    .orElseThrow(() -> new ResourceNotFoundException(\""
                            + reference.targetClassName() + " no encontrado: \" + request."
                            + reference.dtoFieldName() + "())));\n"
                            + "        }");
                }
            }
            return statements;
        }

        private List<String> responseArguments() {
            List<String> arguments = new ArrayList<>();
            arguments.add("entity." + NameUtils.getterName(idFieldName) + "()");
            for (BackendModel.Field field : fields) {
                arguments.add(", entity." + field.getterName() + "()");
            }
            for (BackendModel.Reference reference : references) {
                if (reference.collection()) {
                    arguments.add(", entity." + reference.getterName() + "().stream().map("
                            + reference.targetClassName() + "::getId).toList()");
                } else {
                    arguments.add(", entity." + reference.getterName() + "() == null ? null : entity."
                            + reference.getterName() + "().getId()");
                }
            }
            return arguments;
        }
    }
}
