package backend.xxx.chat.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import backend.xxx.chat.auth.dto.AuthResponse;
import backend.xxx.chat.auth.dto.ChangePasswordRequest;
import backend.xxx.chat.auth.dto.ForgotPasswordRequest;
import backend.xxx.chat.auth.dto.LoginRequest;
import backend.xxx.chat.auth.dto.RefreshTokenRequest;
import backend.xxx.chat.auth.dto.RegisterRequest;
import backend.xxx.chat.auth.dto.RegisterResponse;
import backend.xxx.chat.auth.dto.ResendVerificationRequest;
import backend.xxx.chat.auth.dto.ResetPasswordRequest;
import backend.xxx.chat.auth.dto.VerifyEmailRequest;
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

@Tag(name = "Auth", description = "Authentication and account recovery APIs")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register account")
    @PostMapping("/register")
    @RateLimit(action = "register", maxRequests = 5, timeWindow = 300)
    public ResponseEntity<ResponseData<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseData<>(true, "auth.register.verification.sent", authService.register(request)));
    }

    @Operation(summary = "Login")
    @PostMapping("/login")
    @RateLimit(action = "login", maxRequests = 5, timeWindow = 60)
    public ResponseData<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return new ResponseData<>(true, "auth.login.success", authService.login(request));
    }

    @Operation(summary = "Refresh access token")
    @PostMapping("/refresh")
    @RateLimit(action = "refresh", maxRequests = 10, timeWindow = 60)
    public ResponseData<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return new ResponseData<>(true, "auth.refresh.success", authService.refresh(request));
    }

    @Operation(summary = "Logout")
    @PostMapping("/logout")
    public ResponseData<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return new ResponseData<>(true, "auth.logout.success");
    }

    @Operation(summary = "Request password reset")
    @PostMapping("/forgot-password")
    @RateLimit(action = "forgot-password", maxRequests = 3, timeWindow = 300)
    public ResponseData<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return new ResponseData<>(true, "auth.password.forgot.accepted");
    }

    @Operation(summary = "Reset password")
    @PostMapping("/reset-password")
    @RateLimit(action = "reset-password", maxRequests = 5, timeWindow = 300)
    public ResponseData<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return new ResponseData<>(true, "auth.password.reset.success");
    }


    @Operation(summary = "Verify email")
    @PostMapping("/verify-email")
    @RateLimit(action = "verify-email", maxRequests = 10, timeWindow = 300)
    public ResponseData<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return new ResponseData<>(true, "auth.email.verification.success");
    }

    @Operation(summary = "Resend email verification")
    @PostMapping("/resend-verification")
    @RateLimit(action = "resend-verification", maxRequests = 3, timeWindow = 300)
    public ResponseData<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request);
        return new ResponseData<>(true, "auth.email.verification.accepted");
    }

    @Operation(summary = "Change password")
    @PostMapping("/change-password")
    @RateLimit(action = "change-password", maxRequests = 5, timeWindow = 300)
    public ResponseData<AuthResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return new ResponseData<>(
                true,
                "auth.password.change.success",
                authService.changePassword(request)
        );
    }
}
