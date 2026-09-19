#!/bin/bash

# 获取当前脚本的目录
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"  # 相对路径

# 其他设置
JAR_FILE="note-0.0.1-SNAPSHOT.jar"
SERVER_IP="47.94.149.194"
SERVER_USER="root"
KEY_PATH="~/.ssh/aliyun_server"  # 使用 SSH 密钥
REMOTE_PATH="/tmp"

# 进入项目目录
cd "$PROJECT_DIR" || { echo "项目目录不存在"; exit 1; }

# 执行 Maven 打包
echo "开始打包..."
if mvn clean package; then
    echo "打包成功"
else
    echo "打包失败"
    exit 1
fi

# 计算 JAR 文件的绝对路径
JAR_FILE_PATH="$(pwd)/target/$JAR_FILE"

echo "将要上传的文件: $JAR_FILE_PATH"
echo "目标服务器: $SERVER_USER@$SERVER_IP:$REMOTE_PATH"

# 上传文件到服务器
echo "开始上传文件..."
if scp -i "$KEY_PATH" -o StrictHostKeyChecking=no "$JAR_FILE_PATH" "$SERVER_USER@$SERVER_IP:$REMOTE_PATH"; then
    echo "上传成功"
else
    echo "上传失败"
    exit 1
fi

# SSH 到服务器
echo "连接到服务器..."
ssh -i "$KEY_PATH" "$SERVER_USER@$SERVER_IP" << EOF

# 使用 systemctl 重启服务
echo "重启服务..."
systemctl restart note.service

# 显示日志
echo "显示日志..."
journalctl -u note.service -f

EOF
