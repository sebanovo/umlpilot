package $model.basePackage$.repository;

import $model.basePackage$.model.$entity.className$;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface $entity.className$Repository extends JpaRepository<$entity.className$, $entity.idType$> {
}
