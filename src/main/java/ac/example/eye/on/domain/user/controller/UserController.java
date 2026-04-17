package ac.example.eye.on.domain.user.controller;

import ac.example.eye.on.domain.user.dto.MeResponse;
import ac.example.eye.on.domain.user.service.UserQueryService;
import ac.example.eye.on.global.exception.CustomException;
import ac.example.eye.on.global.exception.ErrorCode;
import ac.example.eye.on.global.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserQueryService userQueryService;

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return userQueryService.getMe(principal.userId());
    }
}

