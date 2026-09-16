package com.umlpilot.umlpilot_api.application.generation;

import java.util.List;

/**
 * Modelo intermedio (view model) que consumen las plantillas. Incluye tanto la
 * información estructurada (entidades, campos, relaciones) como bloques de texto
 * ya resueltos, para que las plantillas solo compongan el archivo.
 *
 * <p>Expone getters estilo JavaBean porque el motor de plantillas resuelve las
 * propiedades mediante métodos {@code getX()/isX()}.</p>
 */
public record BackendModel(
        String basePackage,
        String basePath,
        String applicationName,
        List<String> entityList,
        List<Entity> entities) {

    public String getBasePackage() {
        return basePackage;
    }

    public String getBasePath() {
        return basePath;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public List<String> getEntityList() {
        return entityList;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public record Entity(
            String className,
            String varName,
            String tableName,
            String resourcePath,
            String idFieldName,
            String idType,
            String idGetterName,
            String idSetterName,
            List<Field> fields,
            List<Reference> references,
            List<Method> methods,
            List<DtoField> dtoFields,
            List<String> importStatements,
            List<String> dtoImportStatements,
            List<String> fieldDeclarations,
            List<String> referenceDeclarations,
            List<String> accessorDeclarations,
            List<String> methodDeclarations,
            List<String> dtoRequestComponents,
            List<String> dtoResponseComponents,
            List<String> serviceReferencedImports,
            List<String> serviceFields,
            String constructorParameters,
            List<String> constructorAssignments,
            List<String> applyStatements,
            List<String> responseArguments) {

        public String getClassName() {
            return className;
        }

        public String getVarName() {
            return varName;
        }

        public String getTableName() {
            return tableName;
        }

        public String getResourcePath() {
            return resourcePath;
        }

        public String getIdFieldName() {
            return idFieldName;
        }

        public String getIdType() {
            return idType;
        }

        public String getIdGetterName() {
            return idGetterName;
        }

        public String getIdSetterName() {
            return idSetterName;
        }

        public List<Field> getFields() {
            return fields;
        }

        public List<Reference> getReferences() {
            return references;
        }

        public List<Method> getMethods() {
            return methods;
        }

        public List<DtoField> getDtoFields() {
            return dtoFields;
        }

        public List<String> getImportStatements() {
            return importStatements;
        }

        public List<String> getDtoImportStatements() {
            return dtoImportStatements;
        }

        public List<String> getFieldDeclarations() {
            return fieldDeclarations;
        }

        public List<String> getReferenceDeclarations() {
            return referenceDeclarations;
        }

        public List<String> getAccessorDeclarations() {
            return accessorDeclarations;
        }

        public List<String> getMethodDeclarations() {
            return methodDeclarations;
        }

        public List<String> getDtoRequestComponents() {
            return dtoRequestComponents;
        }

        public List<String> getDtoResponseComponents() {
            return dtoResponseComponents;
        }

        public List<String> getServiceReferencedImports() {
            return serviceReferencedImports;
        }

        public List<String> getServiceFields() {
            return serviceFields;
        }

        public String getConstructorParameters() {
            return constructorParameters;
        }

        public List<String> getConstructorAssignments() {
            return constructorAssignments;
        }

        public List<String> getApplyStatements() {
            return applyStatements;
        }

        public List<String> getResponseArguments() {
            return responseArguments;
        }
    }

    public record Field(
            String name,
            String javaType,
            String columnName,
            String importFqcn,
            String getterName,
            String setterName) {
    }

    public record Reference(
            String fieldName,
            String getterName,
            String setterName,
            String kind,
            boolean manyToOne,
            boolean oneToOne,
            boolean manyToMany,
            boolean collection,
            boolean required,
            String targetClassName,
            String targetVarName,
            String targetIdType,
            String joinColumnName,
            String joinTableName,
            String joinTableJoinColumn,
            String joinTableInverseColumn,
            String dtoFieldName) {
    }

    public record Method(
            String name,
            String returnType,
            String parameters,
            boolean returnsVoid) {
    }

    public record DtoField(
            String name,
            String javaType,
            String importFqcn,
            String validation,
            String validationImport) {
    }
}
