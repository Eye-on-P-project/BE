package ac.example.eye.on.domain.auth.service;

import ac.example.eye.on.domain.auth.dto.LoginRequest;
import ac.example.eye.on.domain.auth.dto.SignupRequest;
import ac.example.eye.on.domain.auth.model.ClientType;

public interface AuthService {

    AuthResult signup(SignupRequest request, ClientType clientType);

    AuthResult login(LoginRequest request, ClientType clientType);

    AuthResult refresh(String refreshToken, ClientType clientType);

    void logout(String accessToken, String refreshToken, ClientType clientType);
}

