package org.example.common.service.impl;

import org.example.common.pojo.User;
import org.example.common.service.UserService;

import java.util.Random;
import java.util.UUID;

public class UserServiceImpl implements UserService {
    @Override
    public User getUserByUserId(Integer id) {
        System.out.println("查询id: " + id);
        Random random = new Random();
        User user = User.builder().id(id).username(UUID.randomUUID().toString()).sex(random.nextBoolean()).build();
        return user;
    }

    @Override
    public Integer insertUserId(User user) {
        System.out.println("插入数据成功：" + user.getUsername());
        return user.getId();
    }
}
