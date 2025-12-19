-- 添加复合索引以优化常见查询

-- 文档表复合索引
-- 优化按用户ID和文件夹ID查询
CREATE INDEX idx_documents_user_folder ON documents(user_id, folder_id);

-- 优化按用户ID和文件类型查询
CREATE INDEX idx_documents_user_type ON documents(user_id, file_type);

-- 优化按用户ID、文件夹ID和文件类型查询
CREATE INDEX idx_documents_user_folder_type ON documents(user_id, folder_id, file_type);

-- 优化按用户ID和创建时间排序查询
CREATE INDEX idx_documents_user_created ON documents(user_id, created_at DESC);

-- 文件夹表复合索引
-- 优化按用户ID和父文件夹ID查询
CREATE INDEX idx_folders_user_parent ON folders(user_id, parent_id);
