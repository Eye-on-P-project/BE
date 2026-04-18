package ac.jwooo.eye_on.domain.user.domain.service;

import ac.jwooo.eye_on.domain.user.application.dto.response.MeResponse;

public interface UserQueryService {

    MeResponse getMe(Long userId);
}

