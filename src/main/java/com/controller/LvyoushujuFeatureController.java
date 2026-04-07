package com.controller;

import com.annotation.IgnoreAuth;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.entity.CityprofileEntity;
import com.entity.LvyoushujuEntity;
import com.entity.StoreupEntity;
import com.entity.UserBehaviorEntity;
import com.service.CityprofileService;
import com.service.LvyoushujuService;
import com.service.StoreupService;
import com.service.UserBehaviorService;
import com.utils.CommonUtil;
import com.utils.PageUtils;
import com.utils.R;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/lvyoushuju")
public class LvyoushujuFeatureController {
    private static final List<String> EXPORT_HEADERS = Arrays.asList("城市", "景点名", "景点类型", "评论人数", "攻略数量", "排名", "热度", "宜居性", "星级", "经度", "纬度", "封面图片", "简介", "点击次数", "推荐说明", "城市画像");

    @Autowired
    private LvyoushujuService lvyoushujuService;
    @Autowired
    private StoreupService storeupService;
    @Autowired
    private CityprofileService cityprofileService;
    @Autowired
    private UserBehaviorService userBehaviorService;

    @IgnoreAuth
    @RequestMapping("/recommend")
    public R recommend(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        int page = parseInt(params.get("page"), 1);
        int limit = parseInt(params.get("limit"), 10);
        Long userId = getUserId(request);
        List<LvyoushujuEntity> allSpots = getAllSpots();
        List<LvyoushujuEntity> recommendations = buildRecommendations(userId, allSpots);
        int fromIndex = Math.max(0, (page - 1) * limit);
        int toIndex = Math.min(recommendations.size(), fromIndex + limit);
        List<LvyoushujuEntity> pageList = fromIndex >= recommendations.size() ? new ArrayList<LvyoushujuEntity>() : recommendations.subList(fromIndex, toIndex);
        PageUtils pageUtils = new PageUtils(new ArrayList<Object>(pageList), recommendations.size(), limit, page);
        return R.ok().put("data", pageUtils);
    }

    @IgnoreAuth
    @RequestMapping("/behavior/{id}")
    public R behavior(@PathVariable("id") Long id, @RequestParam(defaultValue = "click") String type, HttpServletRequest request) {
        LvyoushujuEntity spot = lvyoushujuService.selectById(id);
        if (spot == null) {
            return R.error("景点不存在");
        }
        saveBehavior(request, spot, type, behaviorWeight(type));
        return R.ok();
    }

    @IgnoreAuth
    @RequestMapping("/visited/{id}")
    public R visited(@PathVariable("id") Long id, HttpServletRequest request) {
        LvyoushujuEntity spot = lvyoushujuService.selectById(id);
        if (spot == null) {
            return R.error("景点不存在");
        }
        saveBehavior(request, spot, "visited", 5);
        return R.ok();
    }

    @RequestMapping("/importData")
    public R importData(@RequestParam("file") MultipartFile file) {
        return R.ok().put("data", doImport(file));
    }

    @IgnoreAuth
    @RequestMapping("/exportData")
    public void exportData(@RequestParam Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String mode = StringUtils.defaultIfBlank((String) params.get("mode"), "query");
        String format = StringUtils.defaultIfBlank((String) params.get("format"), "csv");
        List<LvyoushujuEntity> data;
        if ("recommend".equalsIgnoreCase(mode)) {
            data = buildRecommendations(getUserId(request), getAllSpots());
        } else {
            data = lvyoushujuService.selectList(buildExportWrapper(params));
            enrichSpotList(data);
        }
        if ("xls".equalsIgnoreCase(format)) {
            exportExcel(data, response);
        } else {
            exportCsv(data, response);
        }
    }

    @IgnoreAuth
    @RequestMapping("/visualization")
    public R visualization(@RequestParam(required = false) String city, @RequestParam(defaultValue = "paiming") String metric, @RequestParam(defaultValue = "10") Integer limit) {
        List<LvyoushujuEntity> list = getAllSpots().stream()
                .filter(item -> StringUtils.isBlank(city) || city.equals(item.getDiming()))
                .sorted(sortComparator(metric))
                .limit(limit)
                .collect(Collectors.toList());
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < list.size(); i++) {
            LvyoushujuEntity item = list.get(i);
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("rank", i + 1);
            row.put("name", item.getJingdianming());
            row.put("city", item.getDiming());
            row.put("type", item.getJingdianleixing());
            row.put("value", metricValue(item, metric));
            data.add(row);
        }
        return R.ok().put("data", data);
    }

    @IgnoreAuth
    @RequestMapping("/rankings")
    public R rankings() {
        List<LvyoushujuEntity> all = getAllSpots();
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("hotSpotRanking", buildSpotRanking(all, "redu", 10));
        result.put("commentRanking", buildSpotRanking(all, "pinglunrenshu", 10));
        result.put("compositeRanking", buildCompositeRanking(all, 10));
        result.put("cityHotRanking", buildCityRanking("redu", 10));
        result.put("cityLivabilityRanking", buildCityRanking("yijuxing", 10));
        result.put("cities", all.stream().map(LvyoushujuEntity::getDiming).filter(StringUtils::isNotBlank).distinct().sorted().collect(Collectors.toList()));
        return R.ok().put("data", result);
    }

    private EntityWrapper<LvyoushujuEntity> buildExportWrapper(Map<String, Object> params) {
        EntityWrapper<LvyoushujuEntity> wrapper = new EntityWrapper<LvyoushujuEntity>();
        addStringCondition(wrapper, "diming", params.get("diming"));
        addStringCondition(wrapper, "jingdianming", params.get("jingdianming"));
        addStringCondition(wrapper, "jingdianleixing", params.get("jingdianleixing"));
        addRangeCondition(wrapper, "paiming", params);
        addRangeCondition(wrapper, "pinglunrenshu", params);
        addRangeCondition(wrapper, "redu", params);
        addRangeCondition(wrapper, "yijuxing", params);
        String sort = StringUtils.defaultIfBlank((String) params.get("sort"), "id");
        String order = StringUtils.defaultIfBlank((String) params.get("order"), "desc");
        if ("asc".equalsIgnoreCase(order)) {
            wrapper.orderAsc(Collections.singletonList(sort));
        } else {
            wrapper.orderDesc(Collections.singletonList(sort));
        }
        return wrapper;
    }

    private Map<String, Object> doImport(MultipartFile file) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        List<String> errors = new ArrayList<String>();
        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        try {
            if (fileName.endsWith(".csv")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
                String line;
                int index = 0;
                List<String> headers = new ArrayList<String>();
                while ((line = reader.readLine()) != null) {
                    index++;
                    List<String> cols = parseCsv(line);
                    if (index == 1) {
                        headers = cols;
                        continue;
                    }
                    int[] stat = upsertRow(toRowMap(headers, cols), index, errors);
                    inserted += stat[0];
                    updated += stat[1];
                    skipped += stat[2];
                }
            } else {
                InputStream inputStream = file.getInputStream();
                Workbook workbook = WorkbookFactory.create(inputStream);
                Sheet sheet = workbook.getSheetAt(0);
                List<String> headers = new ArrayList<String>();
                for (int i = 0; i < sheet.getPhysicalNumberOfRows(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) {
                        continue;
                    }
                    if (i == 0) {
                        for (int j = 0; j < row.getLastCellNum(); j++) {
                            headers.add(CommonUtil.getCellValue(row.getCell(j)));
                        }
                        continue;
                    }
                    Map<String, String> rowMap = new LinkedHashMap<String, String>();
                    for (int j = 0; j < headers.size(); j++) {
                        rowMap.put(headers.get(j), CommonUtil.getCellValue(row.getCell(j)));
                    }
                    int[] stat = upsertRow(rowMap, i + 1, errors);
                    inserted += stat[0];
                    updated += stat[1];
                    skipped += stat[2];
                }
                inputStream.close();
            }
        } catch (InvalidFormatException e) {
            errors.add("Excel格式错误: " + e.getMessage());
        } catch (IOException e) {
            errors.add("文件读取失败: " + e.getMessage());
        }
        result.put("inserted", inserted);
        result.put("updated", updated);
        result.put("skipped", skipped);
        result.put("errors", errors);
        return result;
    }

    private int[] upsertRow(Map<String, String> rowMap, int rowNum, List<String> errors) {
        LvyoushujuEntity entity = buildEntity(rowMap);
        if (entity == null) {
            errors.add("第" + rowNum + "行缺少必填字段");
            return new int[]{0, 0, 1};
        }
        if (entity.getRedu() == null) {
            entity.setRedu(metricHeat(entity));
        }
        if (entity.getPinglunrenshu() == null) entity.setPinglunrenshu(0);
        if (entity.getGonglveshuliang() == null) entity.setGonglveshuliang(0);
        if (entity.getPaiming() == null) entity.setPaiming(0);
        if (entity.getYijuxing() == null) entity.setYijuxing(0);
        if (entity.getClicknum() == null) entity.setClicknum(0);
        try {
            LvyoushujuEntity old = lvyoushujuService.selectOne(new EntityWrapper<LvyoushujuEntity>().eq("diming", entity.getDiming()).eq("jingdianming", entity.getJingdianming()));
            if (old != null) {
                entity.setId(old.getId());
                if (StringUtils.isBlank(entity.getJianjie())) entity.setJianjie(old.getJianjie());
                if (StringUtils.isBlank(entity.getFengmiantupian())) entity.setFengmiantupian(old.getFengmiantupian());
                lvyoushujuService.updateById(entity);
                return new int[]{0, 1, 0};
            }
            entity.setId(generateId());
            lvyoushujuService.insert(entity);
            return new int[]{1, 0, 0};
        } catch (Exception e) {
            errors.add("第" + rowNum + "行处理失败: " + e.getMessage());
            return new int[]{0, 0, 1};
        }
    }

    private LvyoushujuEntity buildEntity(Map<String, String> rowMap) {
        String city = firstNotBlank(rowMap, "城市", "地名", "diming");
        String name = firstNotBlank(rowMap, "景点名", "景点名称", "jingdianming");
        if (StringUtils.isBlank(city) || StringUtils.isBlank(name)) {
            return null;
        }
        LvyoushujuEntity entity = new LvyoushujuEntity();
        entity.setDiming(city.trim());
        entity.setJingdianming(name.trim());
        entity.setJingdianleixing(firstNotBlank(rowMap, "景点类型", "类型", "jingdianleixing"));
        entity.setPinglunrenshu(parseInteger(firstNotBlank(rowMap, "评论人数", "pinglunrenshu")));
        entity.setGonglveshuliang(parseInteger(firstNotBlank(rowMap, "攻略数量", "gonglveshuliang")));
        entity.setPaiming(parseInteger(firstNotBlank(rowMap, "排名", "paiming")));
        entity.setRedu(parseInteger(firstNotBlank(rowMap, "热度", "redu")));
        entity.setYijuxing(parseInteger(firstNotBlank(rowMap, "宜居性", "yijuxing")));
        entity.setXingji(firstNotBlank(rowMap, "星级", "xingji"));
        entity.setJingdu(firstNotBlank(rowMap, "经度", "jingdu"));
        entity.setWeidu(firstNotBlank(rowMap, "纬度", "weidu"));
        entity.setFengmiantupian(firstNotBlank(rowMap, "封面图片", "fengmiantupian"));
        entity.setJianjie(firstNotBlank(rowMap, "简介", "jianjie"));
        entity.setClicknum(parseInteger(firstNotBlank(rowMap, "点击次数", "clicknum")));
        return entity;
    }

    private List<LvyoushujuEntity> buildRecommendations(Long userId, List<LvyoushujuEntity> allSpots) {
        Map<Long, LvyoushujuEntity> spotMap = allSpots.stream().collect(Collectors.toMap(LvyoushujuEntity::getId, item -> item, (a, b) -> a));
        if (userId == null) {
            return fallbackRecommendations(allSpots, Collections.<String>emptySet(), Collections.<String>emptySet());
        }
        Map<Long, Double> history = buildUserHistory(userId);
        Set<Long> seen = new HashSet<Long>(history.keySet());
        Map<Long, Map<Long, Double>> userItemMatrix = buildUserItemMatrix();
        Map<Long, Map<Long, Double>> itemUserMatrix = transpose(userItemMatrix);
        Map<Long, Double> scoreMap = new HashMap<Long, Double>();
        for (Long historyItemId : history.keySet()) {
            for (Long candidateId : spotMap.keySet()) {
                if (seen.contains(candidateId) || historyItemId.equals(candidateId)) {
                    continue;
                }
                double similarity = similarity(historyItemId, candidateId, itemUserMatrix);
                if (similarity <= 0) {
                    continue;
                }
                scoreMap.put(candidateId, scoreMap.containsKey(candidateId) ? scoreMap.get(candidateId) + similarity * history.get(historyItemId) : similarity * history.get(historyItemId));
            }
        }
        Set<String> preferredCities = buildPreferredCities(userId);
        Set<String> preferredTypes = buildPreferredTypes(userId);
        List<LvyoushujuEntity> recommendList = scoreMap.entrySet().stream().sorted((a, b) -> Double.compare(b.getValue(), a.getValue())).map(entry -> {
            LvyoushujuEntity item = spotMap.get(entry.getKey());
            if (item != null) {
                item.setRecommendReason("协同过滤匹配分 " + String.format(Locale.CHINA, "%.2f", entry.getValue()));
            }
            return item;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        LinkedHashMap<Long, LvyoushujuEntity> merged = new LinkedHashMap<Long, LvyoushujuEntity>();
        for (LvyoushujuEntity item : recommendList) {
            merged.put(item.getId(), item);
        }
        for (LvyoushujuEntity item : fallbackRecommendations(allSpots, preferredCities, preferredTypes)) {
            if (!seen.contains(item.getId())) {
                merged.putIfAbsent(item.getId(), item);
            }
        }
        return new ArrayList<LvyoushujuEntity>(merged.values());
    }

    private List<LvyoushujuEntity> fallbackRecommendations(List<LvyoushujuEntity> allSpots, Set<String> preferredCities, Set<String> preferredTypes) {
        return allSpots.stream().sorted((a, b) -> Double.compare(compositeScore(b, preferredCities, preferredTypes), compositeScore(a, preferredCities, preferredTypes))).peek(item -> {
            if (StringUtils.isBlank(item.getRecommendReason())) {
                item.setRecommendReason("热门兜底推荐");
            }
        }).collect(Collectors.toList());
    }

    private Map<Long, Double> buildUserHistory(Long userId) {
        Map<Long, Double> history = new HashMap<Long, Double>();
        for (UserBehaviorEntity behavior : userBehaviorService.selectList(new EntityWrapper<UserBehaviorEntity>().eq("userid", userId))) {
            double weight = behavior.getBehaviorWeight() == null ? 1D : behavior.getBehaviorWeight();
            if (!history.containsKey(behavior.getJingdianid()) || history.get(behavior.getJingdianid()) < weight) {
                history.put(behavior.getJingdianid(), weight);
            }
        }
        for (StoreupEntity storeup : storeupService.selectList(new EntityWrapper<StoreupEntity>().eq("userid", userId).eq("tablename", "lvyoushuju").eq("type", 1))) {
            history.put(storeup.getRefid(), Math.max(history.containsKey(storeup.getRefid()) ? history.get(storeup.getRefid()) : 0D, 4D));
        }
        return history;
    }

    private Map<Long, Map<Long, Double>> buildUserItemMatrix() {
        Map<Long, Map<Long, Double>> matrix = new HashMap<Long, Map<Long, Double>>();
        for (UserBehaviorEntity behavior : userBehaviorService.selectList(new EntityWrapper<UserBehaviorEntity>())) {
            if (behavior.getUserid() == null || behavior.getJingdianid() == null) {
                continue;
            }
            Map<Long, Double> row = matrix.containsKey(behavior.getUserid()) ? matrix.get(behavior.getUserid()) : new HashMap<Long, Double>();
            double old = row.containsKey(behavior.getJingdianid()) ? row.get(behavior.getJingdianid()) : 0D;
            row.put(behavior.getJingdianid(), Math.max(old, behavior.getBehaviorWeight() == null ? 1D : behavior.getBehaviorWeight()));
            matrix.put(behavior.getUserid(), row);
        }
        for (StoreupEntity storeup : storeupService.selectList(new EntityWrapper<StoreupEntity>().eq("tablename", "lvyoushuju").eq("type", 1))) {
            if (storeup.getUserid() == null || storeup.getRefid() == null) {
                continue;
            }
            Map<Long, Double> row = matrix.containsKey(storeup.getUserid()) ? matrix.get(storeup.getUserid()) : new HashMap<Long, Double>();
            double old = row.containsKey(storeup.getRefid()) ? row.get(storeup.getRefid()) : 0D;
            row.put(storeup.getRefid(), Math.max(old, 4D));
            matrix.put(storeup.getUserid(), row);
        }
        return matrix;
    }

    private Map<Long, Map<Long, Double>> transpose(Map<Long, Map<Long, Double>> userItemMatrix) {
        Map<Long, Map<Long, Double>> result = new HashMap<Long, Map<Long, Double>>();
        for (Map.Entry<Long, Map<Long, Double>> userEntry : userItemMatrix.entrySet()) {
            for (Map.Entry<Long, Double> itemEntry : userEntry.getValue().entrySet()) {
                Map<Long, Double> itemRow = result.containsKey(itemEntry.getKey()) ? result.get(itemEntry.getKey()) : new HashMap<Long, Double>();
                itemRow.put(userEntry.getKey(), itemEntry.getValue());
                result.put(itemEntry.getKey(), itemRow);
            }
        }
        return result;
    }

    private double similarity(Long itemA, Long itemB, Map<Long, Map<Long, Double>> itemUserMatrix) {
        Map<Long, Double> a = itemUserMatrix.get(itemA);
        Map<Long, Double> b = itemUserMatrix.get(itemB);
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0D;
        }
        double dot = 0D;
        double normA = 0D;
        double normB = 0D;
        for (Double value : a.values()) normA += value * value;
        for (Double value : b.values()) normB += value * value;
        for (Map.Entry<Long, Double> entry : a.entrySet()) {
            if (b.containsKey(entry.getKey())) {
                dot += entry.getValue() * b.get(entry.getKey());
            }
        }
        if (dot <= 0D || normA == 0D || normB == 0D) {
            return 0D;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private Set<String> buildPreferredCities(Long userId) {
        return userBehaviorService.selectList(new EntityWrapper<UserBehaviorEntity>().eq("userid", userId)).stream().map(UserBehaviorEntity::getDiming).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
    }

    private Set<String> buildPreferredTypes(Long userId) {
        return userBehaviorService.selectList(new EntityWrapper<UserBehaviorEntity>().eq("userid", userId)).stream().map(UserBehaviorEntity::getJingdianleixing).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
    }

    private double compositeScore(LvyoushujuEntity item, Set<String> preferredCities, Set<String> preferredTypes) {
        double score = metricValue(item, "redu") * 1.6 + metricValue(item, "pinglunrenshu") * 0.02 + metricValue(item, "clicknum") * 0.08 + metricValue(item, "gonglveshuliang") * 0.1 + metricValue(item, "yijuxing") * 0.8 - metricValue(item, "paiming") * 0.2;
        if (preferredCities.contains(item.getDiming())) score += 25;
        if (preferredTypes.contains(item.getJingdianleixing())) score += 20;
        return score;
    }

    private List<Map<String, Object>> buildSpotRanking(List<LvyoushujuEntity> all, String metric, int limit) {
        return all.stream().sorted(sortComparator(metric)).limit(limit).map(item -> {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("name", item.getJingdianming());
            row.put("city", item.getDiming());
            row.put("type", item.getJingdianleixing());
            row.put("value", metricValue(item, metric));
            return row;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildCompositeRanking(List<LvyoushujuEntity> all, int limit) {
        return all.stream().sorted((a, b) -> Double.compare(compositeScore(b, Collections.<String>emptySet(), Collections.<String>emptySet()), compositeScore(a, Collections.<String>emptySet(), Collections.<String>emptySet()))).limit(limit).map(item -> {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("name", item.getJingdianming());
            row.put("city", item.getDiming());
            row.put("type", item.getJingdianleixing());
            row.put("value", compositeScore(item, Collections.<String>emptySet(), Collections.<String>emptySet()));
            return row;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildCityRanking(String metric, int limit) {
        List<CityprofileEntity> profiles = cityprofileService.selectList(new EntityWrapper<CityprofileEntity>());
        if (!profiles.isEmpty()) {
            return profiles.stream().sorted((a, b) -> Double.compare(metricValue(b, metric), metricValue(a, metric))).limit(limit).map(item -> {
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                row.put("name", item.getChengshi());
                row.put("value", "yijuxing".equals(metric) ? item.getYijuxing() : item.getRedu());
                row.put("tags", buildCityTags(item));
                return row;
            }).collect(Collectors.toList());
        }
        Map<String, List<LvyoushujuEntity>> grouped = getAllSpots().stream().collect(Collectors.groupingBy(LvyoushujuEntity::getDiming));
        return grouped.entrySet().stream().map(entry -> {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("name", entry.getKey());
            row.put("value", "yijuxing".equals(metric) ? entry.getValue().stream().mapToInt(item -> parseInt(item.getYijuxing(), 0)).average().orElse(0D) : entry.getValue().stream().mapToInt(item -> parseInt(item.getRedu(), metricHeat(item))).average().orElse(0D));
            return row;
        }).sorted((a, b) -> Double.compare(Double.parseDouble(b.get("value").toString()), Double.parseDouble(a.get("value").toString()))).limit(limit).collect(Collectors.toList());
    }

    private Comparator<LvyoushujuEntity> sortComparator(String metric) {
        if ("paiming".equals(metric)) {
            return Comparator.comparingDouble(item -> metricValue(item, metric));
        }
        return (a, b) -> Double.compare(metricValue(b, metric), metricValue(a, metric));
    }

    private double metricValue(LvyoushujuEntity item, String metric) {
        if ("pinglunrenshu".equals(metric)) return parseInt(item.getPinglunrenshu(), 0);
        if ("gonglveshuliang".equals(metric)) return parseInt(item.getGonglveshuliang(), 0);
        if ("paiming".equals(metric)) return parseInt(item.getPaiming(), Integer.MAX_VALUE / 4);
        if ("yijuxing".equals(metric)) return parseInt(item.getYijuxing(), 0);
        if ("clicknum".equals(metric)) return parseInt(item.getClicknum(), 0);
        return parseInt(item.getRedu(), metricHeat(item));
    }

    private double metricValue(CityprofileEntity item, String metric) {
        return "yijuxing".equals(metric) ? parseInt(item.getYijuxing(), 0) : parseInt(item.getRedu(), 0);
    }

    private List<LvyoushujuEntity> getAllSpots() {
        List<LvyoushujuEntity> list = lvyoushujuService.selectList(new EntityWrapper<LvyoushujuEntity>());
        enrichSpotList(list);
        return list;
    }

    private void enrichSpotList(List<LvyoushujuEntity> list) {
        for (LvyoushujuEntity item : list) {
            CityprofileEntity profile = cityprofileService.selectOne(new EntityWrapper<CityprofileEntity>().eq("chengshi", item.getDiming()));
            if (profile != null) {
                item.setCityTags(buildCityTags(profile));
                if ((item.getYijuxing() == null || item.getYijuxing() == 0) && profile.getYijuxing() != null) item.setYijuxing(profile.getYijuxing());
                if ((item.getRedu() == null || item.getRedu() == 0) && profile.getRedu() != null) item.setRedu(Math.max(profile.getRedu(), metricHeat(item)));
            } else {
                item.setCityTags("");
            }
            if (item.getRedu() == null || item.getRedu() == 0) item.setRedu(metricHeat(item));
        }
    }

    private String buildCityTags(CityprofileEntity profile) {
        if (StringUtils.isNotBlank(profile.getBiaoqian())) {
            return profile.getBiaoqian();
        }
        List<String> tags = new ArrayList<String>();
        if (parseInt(profile.getZiranjingguan(), 0) > 0) tags.add("自然景观");
        if (parseInt(profile.getLishirenwen(), 0) > 0) tags.add("历史人文");
        if (parseInt(profile.getHaibindujia(), 0) > 0) tags.add("海滨度假");
        if (parseInt(profile.getMeishixiuxian(), 0) > 0) tags.add("美食休闲");
        if (parseInt(profile.getDushilvyou(), 0) > 0) tags.add("都市旅游");
        if (parseInt(profile.getRedu(), 0) >= 70) tags.add("热度高");
        if (parseInt(profile.getYijuxing(), 0) >= 70) tags.add("宜居性高");
        return String.join("、", tags);
    }

    private void saveBehavior(HttpServletRequest request, LvyoushujuEntity spot, String type, int weight) {
        Long userId = getUserId(request);
        if (userId == null) return;
        UserBehaviorEntity behavior = new UserBehaviorEntity();
        behavior.setId(generateId());
        behavior.setUserid(userId);
        behavior.setJingdianid(spot.getId());
        behavior.setBehaviorType(type);
        behavior.setBehaviorWeight(weight);
        behavior.setDiming(spot.getDiming());
        behavior.setJingdianleixing(spot.getJingdianleixing());
        userBehaviorService.insert(behavior);
    }

    private void addStringCondition(EntityWrapper<LvyoushujuEntity> wrapper, String column, Object value) {
        if (value == null || StringUtils.isBlank(value.toString())) return;
        String text = value.toString();
        if (text.contains("%")) wrapper.like(column, text.replace("%", ""));
        else wrapper.eq(column, text);
    }

    private void addRangeCondition(EntityWrapper<LvyoushujuEntity> wrapper, String column, Map<String, Object> params) {
        Object start = params.get(column + "_start");
        Object end = params.get(column + "_end");
        if (start != null && StringUtils.isNotBlank(start.toString())) wrapper.ge(column, start);
        if (end != null && StringUtils.isNotBlank(end.toString())) wrapper.le(column, end);
    }

    private void exportCsv(List<LvyoushujuEntity> data, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("旅游景点导出.csv", "UTF-8"));
        StringBuilder builder = new StringBuilder("\uFEFF").append(String.join(",", EXPORT_HEADERS)).append("\n");
        for (LvyoushujuEntity item : data) {
            builder.append(csv(item.getDiming())).append(",").append(csv(item.getJingdianming())).append(",").append(csv(item.getJingdianleixing())).append(",").append(csv(item.getPinglunrenshu())).append(",").append(csv(item.getGonglveshuliang())).append(",").append(csv(item.getPaiming())).append(",").append(csv(item.getRedu())).append(",").append(csv(item.getYijuxing())).append(",").append(csv(item.getXingji())).append(",").append(csv(item.getJingdu())).append(",").append(csv(item.getWeidu())).append(",").append(csv(item.getFengmiantupian())).append(",").append(csv(item.getJianjie())).append(",").append(csv(item.getClicknum())).append(",").append(csv(item.getRecommendReason())).append(",").append(csv(item.getCityTags())).append("\n");
        }
        response.getWriter().write(builder.toString());
        response.getWriter().flush();
    }

    private void exportExcel(List<LvyoushujuEntity> data, HttpServletResponse response) throws IOException {
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("旅游景点");
        Row header = sheet.createRow(0);
        for (int i = 0; i < EXPORT_HEADERS.size(); i++) header.createCell(i).setCellValue(EXPORT_HEADERS.get(i));
        for (int i = 0; i < data.size(); i++) {
            LvyoushujuEntity item = data.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(string(item.getDiming()));
            row.createCell(1).setCellValue(string(item.getJingdianming()));
            row.createCell(2).setCellValue(string(item.getJingdianleixing()));
            row.createCell(3).setCellValue(parseInt(item.getPinglunrenshu(), 0));
            row.createCell(4).setCellValue(parseInt(item.getGonglveshuliang(), 0));
            row.createCell(5).setCellValue(parseInt(item.getPaiming(), 0));
            row.createCell(6).setCellValue(parseInt(item.getRedu(), 0));
            row.createCell(7).setCellValue(parseInt(item.getYijuxing(), 0));
            row.createCell(8).setCellValue(string(item.getXingji()));
            row.createCell(9).setCellValue(string(item.getJingdu()));
            row.createCell(10).setCellValue(string(item.getWeidu()));
            row.createCell(11).setCellValue(string(item.getFengmiantupian()));
            row.createCell(12).setCellValue(string(item.getJianjie()));
            row.createCell(13).setCellValue(parseInt(item.getClicknum(), 0));
            row.createCell(14).setCellValue(string(item.getRecommendReason()));
            row.createCell(15).setCellValue(string(item.getCityTags()));
        }
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("旅游景点导出.xls", "UTF-8"));
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    private List<String> parseCsv(String line) {
        List<String> result = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') inQuotes = !inQuotes;
            else if (ch == ',' && !inQuotes) {
                result.add(current.toString().replace("\"", ""));
                current.setLength(0);
            } else current.append(ch);
        }
        result.add(current.toString().replace("\"", ""));
        return result;
    }

    private Map<String, String> toRowMap(List<String> headers, List<String> values) {
        Map<String, String> row = new LinkedHashMap<String, String>();
        for (int i = 0; i < headers.size(); i++) row.put(headers.get(i), i < values.size() ? values.get(i) : "");
        return row;
    }

    private String firstNotBlank(Map<String, String> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key) && StringUtils.isNotBlank(row.get(key))) return row.get(key).trim();
        }
        return null;
    }

    private Integer parseInteger(String value) {
        if (StringUtils.isBlank(value)) return null;
        try {
            return (int) Math.round(Double.parseDouble(value));
        } catch (Exception e) {
            return null;
        }
    }

    private int parseInt(Object value, int defaultValue) {
        if (value == null || StringUtils.isBlank(value.toString())) return defaultValue;
        try {
            return (int) Math.round(Double.parseDouble(value.toString()));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Long getUserId(HttpServletRequest request) {
        try {
            Object userId = request.getSession().getAttribute("userId");
            return userId == null ? null : Long.valueOf(userId.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private int behaviorWeight(String type) {
        if ("visited".equalsIgnoreCase(type)) return 5;
        if ("favorite".equalsIgnoreCase(type)) return 4;
        if ("browse".equalsIgnoreCase(type)) return 2;
        return 1;
    }

    private int metricHeat(LvyoushujuEntity item) {
        return parseInt(item.getPinglunrenshu(), 0) / 10 + parseInt(item.getGonglveshuliang(), 0) * 2 + parseInt(item.getClicknum(), 0);
    }

    private Long generateId() {
        return new Date().getTime() + (long) Math.floor(Math.random() * 1000);
    }

    private String csv(Object value) {
        String text = string(value).replace("\r", " ").replace("\n", " ");
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private String string(Object value) {
        return value == null ? "" : value.toString();
    }
}
