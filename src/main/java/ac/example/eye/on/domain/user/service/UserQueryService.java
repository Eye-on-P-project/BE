package ac.example.eye.on.domain.user.service;

import ac.example.eye.on.domain.user.dto.MeResponse;

public interface UserQueryService {

    MeResponse getMe(Long userId);
}

