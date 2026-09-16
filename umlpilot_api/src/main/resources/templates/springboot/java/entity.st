package $model.basePackage$.model;

import jakarta.persistence.*;
$entity.importStatements; separator="\n"$

@Entity
@Table(name = "$entity.tableName$")
public class $entity.className$ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "$entity.idFieldName$")
    private $entity.idType$ $entity.idFieldName$;

$entity.fieldDeclarations; separator="\n"$
$entity.referenceDeclarations; separator="\n"$
$entity.accessorDeclarations; separator="\n"$
$entity.methodDeclarations; separator="\n"$
}
