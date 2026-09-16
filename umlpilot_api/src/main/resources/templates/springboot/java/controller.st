package $model.basePackage$.controller;

import $model.basePackage$.dto.$entity.className$Request;
import $model.basePackage$.dto.$entity.className$Response;
import $model.basePackage$.service.$entity.className$Service;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/$entity.resourcePath$")
public class $entity.className$Controller {

    private final $entity.className$Service $entity.varName$Service;

    public $entity.className$Controller($entity.className$Service $entity.varName$Service) {
        this.$entity.varName$Service = $entity.varName$Service;
    }

    @PostMapping
    public ResponseEntity<$entity.className$Response> create(@Valid @RequestBody $entity.className$Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body($entity.varName$Service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<$entity.className$Response>> findAll() {
        return ResponseEntity.ok($entity.varName$Service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<$entity.className$Response> findById(@PathVariable $entity.idType$ id) {
        return ResponseEntity.ok($entity.varName$Service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<$entity.className$Response> update(@PathVariable $entity.idType$ id,
                                                             @Valid @RequestBody $entity.className$Request request) {
        return ResponseEntity.ok($entity.varName$Service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable $entity.idType$ id) {
        $entity.varName$Service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
