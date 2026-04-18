package ac.jwooo.eye_on.domain.auth.domain.service;

import ac.jwooo.eye_on.domain.auth.application.dto.request.LoginRequest;
import ac.jwooo.eye_on.domain.auth.application.dto.request.SignupRequest;
import ac.jwooo.eye_on.domain.auth.domain.entity.ClientType;

public interface AuthService {

    AuthResult signup(SignupRequest request, ClientType clientType);

    AuthResult login(LoginRequest request, ClientType clientType);

    AuthResult refresh(String refreshToken, ClientType clientType);

    void logout(String accessToken, String refreshToken, ClientType clientType);
}

