

ALTER TABLE `user`
  ADD COLUMN `library_code` varchar(255) NOT NULL DEFAULT '' COMMENT '馆代码',
  ADD COLUMN `role` enum('ADMIN','LIBRARIAN','READER') NOT NULL DEFAULT 'READER' COMMENT '用户角色',
  ADD COLUMN `need_password_change` bit(1) NULL DEFAULT b'1' COMMENT '首次登录需改密码',
  ADD COLUMN `ip` varchar(255) DEFAULT NULL COMMENT '来源IP',
  MODIFY COLUMN `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  MODIFY COLUMN `bio` varchar(255) DEFAULT NULL COMMENT '个性签名/格言',
  MODIFY COLUMN `birthday` datetime(6) DEFAULT NULL COMMENT '生日，日期时间格式',
  MODIFY COLUMN `email` varchar(255) DEFAULT NULL COMMENT '电子邮箱',
  MODIFY COLUMN `gender` tinyint(4) DEFAULT NULL COMMENT '性别，0未知，1男，2女',
  MODIFY COLUMN `idcard` varchar(255) DEFAULT NULL COMMENT '身份证号码',
  MODIFY COLUMN `joinip` varchar(255) DEFAULT NULL COMMENT '注册IP地址',
  MODIFY COLUMN `level` tinyint(4) DEFAULT NULL COMMENT '用户等级',
  MODIFY COLUMN `loginfailure` tinyint(4) DEFAULT NULL COMMENT '登录失败次数',
  MODIFY COLUMN `loginip` varchar(255) DEFAULT NULL COMMENT '登录IP地址',
  MODIFY COLUMN `maxsuccessions` int(11) DEFAULT NULL COMMENT '最大连续登录天数',
  MODIFY COLUMN `mobile` varchar(255) DEFAULT NULL COMMENT '手机号',
  MODIFY COLUMN `nickname` varchar(255) DEFAULT NULL COMMENT '昵称',
  MODIFY COLUMN `openid` varchar(255) DEFAULT NULL COMMENT 'openid',
  MODIFY COLUMN `password` varchar(255) DEFAULT NULL COMMENT '密码（已加密存储）',
  MODIFY COLUMN `prevtime` bigint(20) DEFAULT NULL COMMENT '上次登录时间戳',
  MODIFY COLUMN `privacycheck` int(11) DEFAULT NULL COMMENT '隐私协议是否同意',
  MODIFY COLUMN `realname` varchar(255) DEFAULT NULL COMMENT '真实姓名',
  MODIFY COLUMN `score` int(11) DEFAULT NULL COMMENT '积分',
  MODIFY COLUMN `status` varchar(255) DEFAULT NULL COMMENT '用户状态',
  MODIFY COLUMN `subscribe` int(11) DEFAULT NULL COMMENT '是否关注公众号',
  MODIFY COLUMN `successions` int(11) DEFAULT NULL COMMENT '连续登录天数',
  MODIFY COLUMN `token` varchar(255) DEFAULT NULL COMMENT '用户Token',
  MODIFY COLUMN `uid` varchar(255) DEFAULT NULL COMMENT '上图统一用户UID',
  MODIFY COLUMN `unionid` varchar(255) DEFAULT NULL COMMENT 'unionid',
  MODIFY COLUMN `updatetime` bigint(20) DEFAULT NULL COMMENT '更新时间戳',
  MODIFY COLUMN `userinfo` text DEFAULT NULL COMMENT '用户详细信息JSON',
  MODIFY COLUMN `username` varchar(255) NOT NULL COMMENT '用户名，唯一',
  MODIFY COLUMN `verification` varchar(255) DEFAULT NULL COMMENT '验证状态';

  -- 修改字段类型与长度，保持原长度的同时适当增加
  ALTER TABLE `user_file`
    MODIFY COLUMN `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN `bucket` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '存储桶名',
    MODIFY COLUMN `created_date` datetime(6) NULL DEFAULT NULL COMMENT '上传日期',
    MODIFY COLUMN `delete_flag` bit(1) NULL DEFAULT b'0' COMMENT '状态: 0未删除，1已删除',
    MODIFY COLUMN `uperr` int(11) NULL DEFAULT NULL COMMENT '上传出错标志',
    MODIFY COLUMN `name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件名称',
    MODIFY COLUMN `type` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件类型',
    MODIFY COLUMN `url` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件路径',
    MODIFY COLUMN `md5` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'MD5唯一标识',
    MODIFY COLUMN `uid` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'uid';

  -- 新增二期表字段，适当延长字段长度，并赋予默认值
  ALTER TABLE `user_file`
    ADD COLUMN `doc_id` bigint(20) NULL DEFAULT NULL COMMENT '文档ID',
    ADD COLUMN `folder_id` bigint(20) NULL DEFAULT NULL COMMENT '文件夹ID',
    ADD COLUMN `ip` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '来源IP',
    ADD COLUMN `is_latest` bit(1) NULL DEFAULT b'1' COMMENT '是否最新版本',
    ADD COLUMN `library_code` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '馆代码',
    ADD COLUMN `version_notes` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '版本说明',
    ADD COLUMN `version_number` int(11) NULL DEFAULT NULL COMMENT '版本号',
    ADD COLUMN `share_expire_at` datetime(6) NULL DEFAULT NULL COMMENT '分享过期时间',
    ADD COLUMN `share_token` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分享令牌',
    ADD COLUMN `shared` bit(1) NULL DEFAULT b'0' COMMENT '是否共享',
    ADD COLUMN `user_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '用户ID',
    ADD COLUMN `bucket_id` bigint(20) NULL DEFAULT NULL COMMENT '存储桶ID';

  -- 设置唯一索引（如果需要，根据业务调整）
  -- CREATE INDEX idx_uid ON `user_file` (`uid`);
ALTER TABLE user_file MODIFY COLUMN uid VARCHAR(255) NULL;
ALTER TABLE user_file MODIFY COLUMN typename VARCHAR(255);



CREATE TABLE `user_permission`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_time` bigint(20) NOT NULL,
  `permission_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `resource_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UKmbn9jsgcub4adl7fwj8qr4m8f`(`permission_type`, `resource_id`) USING BTREE,
  UNIQUE INDEX `UKn4h2ohmo09vxhevfyt706aj5r`(`user_id`, `permission_type`, `resource_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;



CREATE TABLE `user_audit_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `action_detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `action_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_time` datetime(6) NULL DEFAULT NULL,
  `ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `library_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_agent` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` bigint(20) NULL DEFAULT NULL,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `signature` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;


CREATE TABLE `share_access`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `active` bit(1) NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `expire_at` datetime(6) NULL DEFAULT NULL,
  `library_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `target_id` bigint(20) NULL DEFAULT NULL,
  `target_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `token_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `enabled` bit(1) NULL DEFAULT NULL,
  `target_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `owner_id` bigint(20) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

CREATE TABLE `share_access_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `access_ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `access_time` datetime(6) NULL DEFAULT NULL,
  `file_id` bigint(20) NULL DEFAULT NULL,
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_agent` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `action_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `library_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `owner_uid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `token_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `signature` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `owner_id` bigint(20) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;


CREATE TABLE `permission_resource`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `resource_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `resource_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

CREATE TABLE `library_site`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `create_time` datetime(6) NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `sort_order` int(11) NULL DEFAULT NULL,
  `status` bit(1) NULL DEFAULT NULL,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `update_time` datetime(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UKtd4wnvr775x5ebobpn7jloi5h`(`code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;


CREATE TABLE `id_sequence`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `current_value` bigint(20) NOT NULL,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK1lxpxh162qablrjdrwxma3yw9`(`type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;


INSERT INTO `id_sequence` VALUES (1, 400, 'doc_id');



CREATE TABLE `folder`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `is_public` bit(1) NULL DEFAULT NULL,
  `library_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `parent_id` bigint(20) NULL DEFAULT NULL,
  `share_expire_at` datetime(6) NULL DEFAULT NULL,
  `share_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `shared` bit(1) NULL DEFAULT NULL,
  `system_folder` bit(1) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `user_id` bigint(20) NOT NULL,
  `created_date` datetime(6) NULL DEFAULT NULL,
  `updated_date` datetime(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

CREATE TABLE `bucket`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `library_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `max_capacity` bigint(20) NULL DEFAULT NULL,
  `owner_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UKhe0xrer6rh4dgaalutt7prhbm`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

CREATE TABLE `bucket_permission`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `bucket_id` bigint(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `permission` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK91cdpkpw56k39ayowwsxx043j`(`user_id`, `bucket_id`) USING BTREE,
  CONSTRAINT `FKciyexqevpggo5jap23lsyf0la` FOREIGN KEY (`bucket_id`) REFERENCES `bucket` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

CREATE TABLE `role_permission`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `permission` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `role` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `resource_id` bigint(20) NOT NULL,
  `role_type` enum('ADMIN','LIBRARIAN','READER') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK4qnq5xg52y2ojsuqifbkvjuvw`(`role`, `resource_id`, `permission`) USING BTREE,
  INDEX `FKr73uqogh34cnr0by35t99kveq`(`resource_id`) USING BTREE,
  CONSTRAINT `FKr73uqogh34cnr0by35t99kveq` FOREIGN KEY (`resource_id`) REFERENCES `permission_resource` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

CREATE TABLE `file_permission` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `permission` varchar(255) NOT NULL,
  `file_id` bigint(20) NOT NULL,
  `user_id` bigint(20) unsigned NOT NULL,  -- 关键修改：添加 unsigned
  PRIMARY KEY (`id`),
  INDEX `idx_file_id` (`file_id`),
  INDEX `idx_user_id` (`user_id`),
  CONSTRAINT `fk_file_permission_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_file_permission_file`
    FOREIGN KEY (`file_id`) REFERENCES `user_file` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;  -- 统一为 unicode_ci

