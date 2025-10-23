package org.example.ai_api.Service.Apis.Commons;

import org.apache.commons.lang3.StringUtils;
import org.example.ai_api.Bean.Entity.UserInfo;
import org.example.ai_api.Service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class UserInfoGetter {

    @Autowired
    private UserInfoService userInfoService;

    public String getDepartmentId(String userId) {
        if (StringUtils.isNotBlank(userId)) {
            UserInfo userInfo = userInfoService.findById(userId);
            if (Objects.nonNull(userInfo)) {
                return userInfo.getDepartmentId();
            }
        }
        return null;
    }
}
