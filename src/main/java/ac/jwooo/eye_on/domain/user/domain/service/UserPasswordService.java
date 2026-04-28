package ac.jwooo.eye_on.domain.user.domain.service;

import ac.jwooo.eye_on.domain.user.application.dto.request.ChangePasswordRequest;

public interface UserPasswordService {

    void changePassword(Long userId, ChangePasswordRequest request);
}
