package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.UserSearchResult;
import com.umlpilot.umlpilot_api.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserSearchService {

    private static final int MIN_QUERY_LENGTH = 2;
    private static final int MAX_RESULTS = 10;

    private final UserRepository userRepository;

    public UserSearchService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserSearchResult> search(String query, String currentUserId) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.length() < MIN_QUERY_LENGTH) {
            return List.of();
        }
        return userRepository.search(normalized, currentUserId, MAX_RESULTS).stream()
                .map(user -> new UserSearchResult(
                        user.getId().value(), user.getEmail(), user.getFirstName(), user.getLastName()))
                .toList();
    }
}
