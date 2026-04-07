package com.service;

import com.baomidou.mybatisplus.service.IService;
import com.entity.CityprofileEntity;
import com.utils.PageUtils;

import java.util.Map;

public interface CityprofileService extends IService<CityprofileEntity> {
    PageUtils queryPage(Map<String, Object> params);
}
