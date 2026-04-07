package com.service.impl;

import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.dao.UserBehaviorDao;
import com.entity.UserBehaviorEntity;
import com.service.UserBehaviorService;
import org.springframework.stereotype.Service;

@Service("userBehaviorService")
public class UserBehaviorServiceImpl extends ServiceImpl<UserBehaviorDao, UserBehaviorEntity> implements UserBehaviorService {
}
