package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import com.umlpilot.umlpilot_api.domain.model.*;
import com.umlpilot.umlpilot_api.domain.repository.ProjectRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ProjectPersistenceAdapter implements ProjectRepository {

    private final ProjectJpaRepository jpaRepository;

    public ProjectPersistenceAdapter(ProjectJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Project save(Project project) {
        ProjectJpaEntity entity = new ProjectJpaEntity(
                project.getId().value(), project.getName(), project.getDescription(),
                project.getCreatorId().value(), project.getStatus(),
                project.getCreatedAt(), project.getUpdatedAt()
        );
        ProjectJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Project> findById(ProjectId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Project> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Project> findByIdAndUserId(String projectId, String userId) {
        ProjectJpaEntity entity = jpaRepository.findByIdAndUserId(projectId, userId);
        return entity != null ? Optional.of(toDomain(entity)) : Optional.empty();
    }

    @Override
    public void delete(ProjectId id) {
        jpaRepository.deleteById(id.value());
    }

    private Project toDomain(ProjectJpaEntity entity) {
        return Project.reconstitute(
                ProjectId.from(entity.getId()), entity.getName(), entity.getDescription(),
                UserId.from(entity.getCreatorId()), entity.getStatus(),
                entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
