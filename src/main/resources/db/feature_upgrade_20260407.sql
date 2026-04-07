ALTER TABLE lvyoushuju
    ADD COLUMN jingdianleixing VARCHAR(64) DEFAULT NULL COMMENT '景点类型',
    ADD COLUMN redu INT DEFAULT 0 COMMENT '热度',
    ADD COLUMN yijuxing INT DEFAULT 0 COMMENT '宜居性';

CREATE TABLE IF NOT EXISTS city_profile (
    id BIGINT NOT NULL,
    chengshi VARCHAR(64) NOT NULL COMMENT '城市',
    biaoqian VARCHAR(255) DEFAULT NULL COMMENT '城市画像标签',
    redu INT DEFAULT 0 COMMENT '城市热度',
    yijuxing INT DEFAULT 0 COMMENT '城市宜居性',
    ziranjingguan INT DEFAULT 0 COMMENT '自然景观画像',
    lishirenwen INT DEFAULT 0 COMMENT '历史人文画像',
    haibindujia INT DEFAULT 0 COMMENT '海滨度假画像',
    meishixiuxian INT DEFAULT 0 COMMENT '美食休闲画像',
    dushilvyou INT DEFAULT 0 COMMENT '都市旅游画像',
    jianjie TEXT COMMENT '画像描述',
    addtime TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_city_profile_chengshi (chengshi)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='城市画像';

CREATE TABLE IF NOT EXISTS user_behavior (
    id BIGINT NOT NULL,
    userid BIGINT NOT NULL COMMENT '用户ID',
    jingdianid BIGINT NOT NULL COMMENT '景点ID',
    behavior_type VARCHAR(32) NOT NULL COMMENT '行为类型',
    behavior_weight INT DEFAULT 1 COMMENT '行为权重',
    diming VARCHAR(64) DEFAULT NULL COMMENT '城市',
    jingdianleixing VARCHAR(64) DEFAULT NULL COMMENT '景点类型',
    addtime TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_behavior_user (userid),
    KEY idx_user_behavior_spot (jingdianid),
    KEY idx_user_behavior_type (behavior_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户景点行为';
