JAR_FILE="nginx.conf"
# 计算 JAR 文件的绝对路径
JAR_FILE_PATH="$(pwd)/$JAR_FILE"
SERVER_IP="47.94.149.194"
SERVER_USER="root"
KEY_PATH="~/.ssh/aliyun_server"  # 使用 SSH 密钥
REMOTE_PATH="/etc/nginx"

echo "上传nginx.config文件到远程服务器..."
echo "将要上传的文件: $JAR_FILE_PATH"
echo "目标服务器: $SERVER_USER@$SERVER_IP:$REMOTE_PATH"

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

# 使用 systemctl 重启nginx服务
echo "重启nginx服务..."
systemctl restart nginx.service

# 显示日志
echo "显示日志..."
journalctl -u nginx.service -f

EOF