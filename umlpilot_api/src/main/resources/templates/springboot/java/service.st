package $model.basePackage$.service;

import $model.basePackage$.dto.$entity.className$Request;
import $model.basePackage$.dto.$entity.className$Response;
import $model.basePackage$.exception.ResourceNotFoundException;
import $model.basePackage$.model.$entity.className$;
import $model.basePackage$.repository.$entity.className$Repository;
$entity.serviceReferencedImports; separator="\n"$
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class $entity.className$Service {

    private final $entity.className$Repository $entity.varName$Repository;
$entity.serviceFields; separator="\n"$

    public $entity.className$Service($entity.constructorParameters$) {
$entity.constructorAssignments; separator="\n"$
    }

    @Transactional(readOnly = true)
    public List<$entity.className$Response> findAll() {
        return $entity.varName$Repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public $entity.className$Response findById($entity.idType$ id) {
        return toResponse($entity.varName$Repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("$entity.className$ no encontrado: " + id)));
    }

    @Transactional
    public $entity.className$Response create($entity.className$Request request) {
        $entity.className$ entity = new $entity.className$();
        apply(entity, request);
        return toResponse($entity.varName$Repository.save(entity));
    }

    @Transactional
    public $entity.className$Response update($entity.idType$ id, $entity.className$Request request) {
        $entity.className$ entity = $entity.varName$Repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("$entity.className$ no encontrado: " + id));
        apply(entity, request);
        return toResponse($entity.varName$Repository.save(entity));
    }

    @Transactional
    public void delete($entity.idType$ id) {
        if (!$entity.varName$Repository.existsById(id)) {
            throw new ResourceNotFoundException("$entity.className$ no encontrado: " + id);
        }
        $entity.varName$Repository.deleteById(id);
    }

    private void apply($entity.className$ entity, $entity.className$Request request) {
$entity.applyStatements; separator="\n"$
    }

    private $entity.className$Response toResponse($entity.className$ entity) {
        return new $entity.className$Response($entity.responseArguments$);
    }
}
