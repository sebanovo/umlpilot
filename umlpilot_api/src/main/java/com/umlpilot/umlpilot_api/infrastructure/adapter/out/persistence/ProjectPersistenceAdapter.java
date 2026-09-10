package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import com.umlpilot.umlpilot_api.domain.model.*;
import com.umlpilot.umlpilot_api.domain.repository.ProjectRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
public class ProjectPersistenceAdapter implements ProjectRepository {
    private final ProjectJpaRepository jpa;
    public ProjectPersistenceAdapter(ProjectJpaRepository jpa) { this.jpa = jpa; }

    @Override public Project save(Project project) {
        Optional<ProjectJpaEntity> existing = jpa.findById(project.getId().value());
        ProjectJpaEntity entity = existing.orElse(new ProjectJpaEntity());
        entity.setName(project.getName());
        entity.setDescription(project.getDescription());
        entity.setCreatorId(project.getCreatorId().value());
        entity.setStatus(project.getStatus());
        return toDomain(jpa.save(entity));
    }
    @Override public Optional<Project> findById(ProjectId id) { return jpa.findById(id.value()).map(this::toDomain); }
    @Override public List<Project> findByUserId(String userId) { return jpa.findByUserId(userId).stream().map(this::toDomain).toList(); }
    @Override public Optional<Project> findByIdAndUserId(String projectId, String userId) {
        ProjectJpaEntity e = jpa.findByIdAndUserId(projectId, userId);
        return e != null ? Optional.of(toDomain(e)) : Optional.empty();
    }
    @Override public void delete(ProjectId id) { jpa.deleteById(id.value()); }
    private Project toDomain(ProjectJpaEntity e) {
        return Project.reconstitute(ProjectId.from(e.getId()), e.getName(), e.getDescription(), UserId.from(e.getCreatorId()), e.getStatus(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
