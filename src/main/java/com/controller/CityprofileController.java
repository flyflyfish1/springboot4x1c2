package com.controller;

import com.annotation.IgnoreAuth;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.entity.CityprofileEntity;
import com.service.CityprofileService;
import com.utils.MPUtil;
import com.utils.PageUtils;
import com.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/cityprofile")
public class CityprofileController {
    @Autowired
    private CityprofileService cityprofileService;

    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, CityprofileEntity cityprofile) {
        EntityWrapper<CityprofileEntity> ew = new EntityWrapper<CityprofileEntity>();
        PageUtils page = cityprofileService.queryPage(params);
        page.setList(cityprofileService.selectList(MPUtil.sort(MPUtil.between(MPUtil.likeOrEq(ew, cityprofile), params), params)));
        page.setTotal(cityprofileService.selectCount(MPUtil.between(MPUtil.likeOrEq(new EntityWrapper<CityprofileEntity>(), cityprofile), params)));
        return R.ok().put("data", page);
    }

    @IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params, CityprofileEntity cityprofile) {
        return page(params, cityprofile);
    }

    @IgnoreAuth
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id) {
        return R.ok().put("data", cityprofileService.selectById(id));
    }

    @IgnoreAuth
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Long id) {
        return info(id);
    }

    @IgnoreAuth
    @RequestMapping("/byCity")
    public R byCity(@RequestParam String chengshi) {
        CityprofileEntity data = cityprofileService.selectOne(new EntityWrapper<CityprofileEntity>().eq("chengshi", chengshi));
        return R.ok().put("data", data);
    }

    @RequestMapping("/save")
    public R save(@RequestBody CityprofileEntity cityprofile, HttpServletRequest request) {
        cityprofile.setId(new Date().getTime() + (long) Math.floor(Math.random() * 1000));
        cityprofileService.insert(cityprofile);
        return R.ok();
    }

    @RequestMapping("/add")
    public R add(@RequestBody CityprofileEntity cityprofile, HttpServletRequest request) {
        return save(cityprofile, request);
    }

    @RequestMapping("/update")
    public R update(@RequestBody CityprofileEntity cityprofile) {
        cityprofileService.updateById(cityprofile);
        return R.ok();
    }

    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids) {
        cityprofileService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
