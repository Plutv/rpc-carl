package org.example.service;

import org.example.common.pojo.User;

public interface UserService {
    User getUserByUserId(Integer id);

    Integer insertUserId(User user);
}
