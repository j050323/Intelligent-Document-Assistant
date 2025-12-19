-- 文件夹表
CREATE TABLE folders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_id BIGINT REFERENCES folders(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    path VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_folder_name_per_parent UNIQUE(user_id, parent_id, name)
);

-- 文件夹表索引
CREATE INDEX idx_folders_user_id ON folders(user_id);
CREATE INDEX idx_folders_parent_id ON folders(parent_id);
CREATE INDEX idx_folders_path ON folders(path);

-- 文档表
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    folder_id BIGINT REFERENCES folders(id) ON DELETE SET NULL,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 文档表索引
CREATE INDEX idx_documents_user_id ON documents(user_id);
CREATE INDEX idx_documents_folder_id ON documents(folder_id);
CREATE INDEX idx_documents_filename ON documents(filename);
CREATE INDEX idx_documents_file_type ON documents(file_type);
CREATE INDEX idx_documents_created_at ON documents(created_at);

-- 添加表注释
COMMENT ON TABLE folders IS '文件夹表，用于组织文档的层级结构';
COMMENT ON TABLE documents IS '文档表，存储用户上传的文档元数据';

-- 添加列注释
COMMENT ON COLUMN folders.user_id IS '文件夹所有者用户ID';
COMMENT ON COLUMN folders.parent_id IS '父文件夹ID，NULL表示根文件夹';
COMMENT ON COLUMN folders.name IS '文件夹名称';
COMMENT ON COLUMN folders.path IS '文件夹完整路径';

COMMENT ON COLUMN documents.user_id IS '文档所有者用户ID';
COMMENT ON COLUMN documents.folder_id IS '所属文件夹ID，NULL表示根目录';
COMMENT ON COLUMN documents.filename IS '存储的文件名（唯一）';
COMMENT ON COLUMN documents.original_filename IS '用户上传时的原始文件名';
COMMENT ON COLUMN documents.file_path IS '文件在存储系统中的路径';
COMMENT ON COLUMN documents.file_type IS '文件类型（pdf, docx, txt等）';
COMMENT ON COLUMN documents.file_size IS '文件大小（字节）';
COMMENT ON COLUMN documents.mime_type IS 'MIME类型';
