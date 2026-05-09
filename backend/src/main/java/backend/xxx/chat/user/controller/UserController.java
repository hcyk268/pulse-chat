package backend.xxx.chat.user.controller;

import backend.xxx.chat.common.security.CurrentUserProvider;
import backend.xxx.chat.user.dto.UpdateMyProfileRequest;
import backend.xxx.chat.user.dto.UserResponse;
import backend.xxx.chat.user.dto.UserSearchResponse;
import backend.xxx.chat.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final CurrentUserProvider currentUserProvider;
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile(currentUserProvider.getCurrentUsername()));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(@Valid @RequestBody UpdateMyProfileRequest request) {
        return ResponseEntity.ok(userService.updateMyProfile(currentUserProvider.getCurrentUsername(), request));
    }

    @GetMapping("/search")
    public ResponseEntity<UserSearchResponse> search(
            @RequestParam(name = "q") String keyword,
            @RequestParam(name = "limit", required = false, defaultValue = "10") Short limit
    ) {
        return ResponseEntity.ok(userService.search(currentUserProvider.getCurrentUsername(), keyword, limit));
    }
}
