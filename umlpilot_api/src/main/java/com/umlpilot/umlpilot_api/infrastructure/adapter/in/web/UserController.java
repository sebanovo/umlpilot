package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.service.UserSearchService;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.response.UserSearchResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserSearchService userSearchService;

    public UserController(UserSearchService userSearchService) {
        this.userSearchService = userSearchService;
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(name = "q", defaultValue = "") String query) {
        return ResponseEntity.ok(userSearchService.search(query, SecurityUtils.getCurrentUserId()).stream()
                .map(result -> new UserSearchResponse(
                        result.id(), result.email(), result.firstName(), result.lastName()))
                .toList());
    }
}
