package ac.jwooo.eye_on.domain.organization.domain.service;

import java.util.List;
import java.util.Map;

import ac.jwooo.eye_on.domain.user.domain.entity.User;

public interface OrganizationMemberUserService {

    User getActiveUserByEmail(String email);

    Map<Long, User> getActiveUsersByIds(List<Long> userIds);
}
