package com.vinsguru.netflux.user.mapper;

import com.vinsguru.netflux.user.dto.RegisterRequest;
import com.vinsguru.netflux.user.entity.User;

public class UserMapper {

    public static User toEntity(RegisterRequest request, String encodedPassword) {
        var user = new User();
        user.setUsername(request.username());
        user.setPassword(encodedPassword);
        user.setName(request.name());
        return user;
    }
}
