package com.umlpilot.umlpilot_api.domain.repository;

import com.umlpilot.umlpilot_api.domain.model.Project;
import com.umlpilot.umlpilot_api.domain.model.ProjectId;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository {
    Project save(Project project);
    Optional<Project> findById(ProjectId id);
    List<Project> findByUserId(String userId);
    Optional<Project> findByIdAndUserId(String projectId, String userId);
    void delete(ProjectId id);
}
