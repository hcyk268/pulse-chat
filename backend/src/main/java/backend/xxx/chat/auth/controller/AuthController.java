package backend.xxx.chat.auth.controller;

import backend.xxx.chat.auth.dto.AuthResponse;
import backend.xxx.chat.auth.dto.LoginRequest;
import backend.xxx.chat.auth.dto.RefreshTokenRequest;
import backend.xxx.chat.auth.dto.RegisterRequest;
import backend.xxx.chat.auth.service.AuthService;
import backend.xxx.chat.common.dto.ResponseData;
import backend.xxx.chat.common.ratelimit.RateLimit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @RateLimit(action = "register", maxRequests = 5, timeWindow = 300)
    public ResponseEntity<ResponseData<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseData<>(true, "auth.register.success", authService.register(request)));
    }

    @PostMapping("/login")
    @RateLimit(action = "login", maxRequests = 5, timeWindow = 60)
    public ResponseData<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return new ResponseData<>(true, "auth.login.success", authService.login(request));
    }

    @PostMapping("/refresh")
    @RateLimit(action = "refresh", maxRequests = 10, timeWindow = 60)
    public ResponseData<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return new ResponseData<>(true, "auth.refresh.success", authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseData<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return new ResponseData<>(true, "auth.logout.success");
    }
}
