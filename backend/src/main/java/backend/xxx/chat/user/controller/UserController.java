package backend.xxx.chat.user.controller;

import backend.xxx.chat.common.dto.ResponseData;
import backend.xxx.chat.common.security.CurrentUserProvider;
import backend.xxx.chat.user.dto.UpdateMyProfileRequest;
import backend.xxx.chat.user.dto.UserResponse;
import backend.xxx.chat.user.dto.UserSearchResponse;
import backend.xxx.chat.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final CurrentUserProvider currentUserProvider;
    private final UserService userService;

    @GetMapping("/me")
    public ResponseData<UserResponse> getMyProfile() {
        return new ResponseData<>(true, "Get personal information successfully", userService.getMyProfile(currentUserProvider.getCurrentUsername()));
    }

    @PatchMapping("/me")
    public ResponseData<UserResponse> updateMyProfile(@Valid @RequestBody UpdateMyProfileRequest request) {
        return new ResponseData<>(true, "Update personal information successfully", userService.updateMyProfile(currentUserProvider.getCurrentUsername(), request));
    }

    @GetMapping("/search")
    public ResponseData<UserSearchResponse> search(
            @RequestParam(name = "q") String keyword,
            @Min(1) @Max(100) @RequestParam(name = "limit", required = false, defaultValue = "10") Short limit
    ) {
        return new ResponseData<>(true, "Search users successfully", userService.search(currentUserProvider.getCurrentUsername(), keyword, limit));
    }
}
