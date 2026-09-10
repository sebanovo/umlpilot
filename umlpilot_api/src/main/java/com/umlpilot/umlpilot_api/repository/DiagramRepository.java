package com.umlpilot.umlpilot_api.repository;

import com.umlpilot.umlpilot_api.model.Diagram;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiagramRepository extends JpaRepository<Diagram, String> {

    List<Diagram> findByProjectId(String projectId);
}
