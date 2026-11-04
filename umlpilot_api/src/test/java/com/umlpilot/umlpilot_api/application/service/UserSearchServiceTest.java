package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.UserSearchResult;
import com.umlpilot.umlpilot_api.domain.model.Role;
import com.umlpilot.umlpilot_api.domain.model.User;
import com.umlpilot.umlpilot_api.domain.model.UserId;
import com.umlpilot.umlpilot_api.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class UserSearchServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserSearchService service = new UserSearchService(userRepository);

    private final User bob = User.reconstitute(UserId.from("bob1"), "bob@x.com", "hash",
            "Bob", "Rey", Role.USER, LocalDateTime.now(), LocalDateTime.now());

    @Test
    void queryShorterThanTwoCharactersReturnsEmptyWithoutHittingRepository() {
        assertTrue(service.search(" a ", "me").isEmpty());
        assertTrue(service.search(null, "me").isEmpty());
        verifyNoInteractions(userRepository);
    }

    @Test
    void trimsQueryAndMapsResults() {
        when(userRepository.search("bob", "me", 10)).thenReturn(List.of(bob));

        List<UserSearchResult> results = service.search("  bob  ", "me");

        assertEquals(1, results.size());
        assertEquals("bob1", results.get(0).id());
        assertEquals("bob@x.com", results.get(0).email());
        assertEquals("Bob", results.get(0).firstName());
    }
}
