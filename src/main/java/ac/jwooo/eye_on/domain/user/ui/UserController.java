package ac.jwooo.eye_on.domain.user.ui;

import java.util.Map;

import ac.jwooo.eye_on.domain.user.application.dto.request.ChangePasswordRequest;
import ac.jwooo.eye_on.domain.user.application.dto.response.MeResponse;
import ac.jwooo.eye_on.domain.user.domain.service.UserPasswordService;
import ac.jwooo.eye_on.domain.user.domain.service.UserQueryService;
import ac.jwooo.eye_on.domain.user.ui.spec.UserControllerSpec;
import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import ac.jwooo.eye_on.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserControllerSpec {

    private final UserQueryService userQueryService;
    private final UserPasswordService userPasswordService;

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        return userQueryService.getMe(extractUserId(authentication));
    }

    @PatchMapping("/me/password")
    public Map<String, Object> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userPasswordService.changePassword(extractUserId(authentication), request);
        return Map.of("success", true);
    }

    private Long extractUserId(Authentication authentication) {
        return extractPrincipal(authentication).userId();
    }

    private AuthenticatedUser extractPrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return principal;
    }
}
