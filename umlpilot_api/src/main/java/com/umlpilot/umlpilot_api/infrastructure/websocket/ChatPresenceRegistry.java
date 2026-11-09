package com.umlpilot.umlpilot_api.infrastructure.websocket;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatPresenceRegistry {

    private final Map<String, Set<String>> usersByProject = new ConcurrentHashMap<>();
    private final Map<String, String> projectBySession = new ConcurrentHashMap<>();
    private final Map<String, String> userBySession = new ConcurrentHashMap<>();

    public void join(String projectId, String userId, String sessionId) {
        usersByProject.computeIfAbsent(projectId, key -> ConcurrentHashMap.newKeySet()).add(userId);
        if (sessionId != null) {
            projectBySession.put(sessionId, projectId);
            userBySession.put(sessionId, userId);
        }
    }

    public void leave(String sessionId) {
        String projectId = projectBySession.remove(sessionId);
        String userId = userBySession.remove(sessionId);
        if (projectId == null || userId == null) return;
        Set<String> users = usersByProject.get(projectId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) usersByProject.remove(projectId);
        }
    }

    public List<String> users(String projectId) {
        return List.copyOf(usersByProject.getOrDefault(projectId, Set.of()));
    }

    public String projectOf(String sessionId) {
        return projectBySession.get(sessionId);
    }
}
