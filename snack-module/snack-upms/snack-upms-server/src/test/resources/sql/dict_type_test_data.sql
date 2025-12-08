-- 测试数据: 字典类型 (使用 DELETE + INSERT 确保每次运行都能正确插入)
DELETE FROM sys_dict_type WHERE id IN (1, 2, 3);
INSERT INTO sys_dict_type (id, dict_name, dict_type, status, sort_order, remark, create_time, update_time)
VALUES (1, '系统状态', 'sys_status', 1, 0, '系统状态', NOW(), NOW()),
       (2, '待删除类型', 'to_delete', 1, 1, '用于删除测试', NOW(), NOW()),
       (3, '禁用类型', 'disabled_type', 0, 2, '禁用状态', NOW(), NOW());

-- 测试数据: 字典数据
DELETE FROM sys_dict_data WHERE id IN (1, 2, 3);
INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, css_class, list_class, is_default, status, sort_order, remark, create_time, update_time)
VALUES (1, 'sys_status', '启用', '1', NULL, 'success', 0, 1, 0, NULL, NOW(), NOW()),
       (2, 'sys_status', '禁用', '0', NULL, 'danger', 0, 1, 1, NULL, NOW(), NOW()),
       (3, 'to_delete', '待删除项', 'delete', NULL, NULL, 0, 1, 0, NULL, NOW(), NOW());
