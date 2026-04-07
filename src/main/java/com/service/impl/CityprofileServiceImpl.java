package com.service.impl;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.dao.CityprofileDao;
import com.entity.CityprofileEntity;
import com.service.CityprofileService;
import com.utils.PageUtils;
import com.utils.Query;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("cityprofileService")
public class CityprofileServiceImpl extends ServiceImpl<CityprofileDao, CityprofileEntity> implements CityprofileService {
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        Page<CityprofileEntity> page = this.selectPage(new Query<CityprofileEntity>(params).getPage(), new EntityWrapper<CityprofileEntity>());
        return new PageUtils(page);
    }
}
