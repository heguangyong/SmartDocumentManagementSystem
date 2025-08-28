# 📄 Smart Document Management System (SDMS) Backend
docker pull maven:3.9.6-eclipse-temurin-21
docker pull eclipse-temurin:21-jdk-jammy
docker system prune -f

# 清理并构建fat jar
mvn clean package -DskipTests

docker build -t sdms-backend:1.0.1 .


# docker 安装redis 端口9736 密码同配置文件中设置

# 第一次需要
docker network create my-network
# onlyoffice
# 跳过认证
docker run -i -t -d --name onlyoffice -p 8081:80 -e JWT_ENABLED=false -e ALLOW_PRIVATE_IP_ADDRESS=true --network my-network onlyoffice/documentserver
# 跳过认证 + 跨域访问
docker rm -f onlyoffice; docker run -d --name onlyoffice -p 8081:80 -e JWT_ENABLED=false -e ALLOW_PRIVATE_IP_ADDRESS=true -e EXTERNAL_HOST=192.168.1.198 -e CORS_ALLOWED_ORIGINS=http://192.168.1.198:8090,http://192.168.1.191:8080 --network my-network onlyoffice/documentserver
# 线上版本的docker 
docker rm -f onlyoffice; docker run -d --name onlyoffice -p 8081:80 -e JWT_ENABLED=false -e ALLOW_PRIVATE_IP_ADDRESS=true -e EXTERNAL_HOST=172.24.88.90 -e CORS_ALLOWED_ORIGINS=http://172.24.88.90:8090,http://172.24.88.90:8081,https://s3mgt.library.sh.cn --network my-network onlyoffice/documentserver


# win11 环境
docker run -d --name sdms-backend -p 8080:8080 --network my-network `
    --add-host=host.docker.internal:192.168.1.198 `
-v E:/yangzhou/SmartDocumentManagementSystem/application.yml:/app/application.yml `
sdms-backend:1.0.12


# centos 环境
docker run -d --name sdms-backend -p 8080:8080 --network my-network \
--add-host=host.docker.internal:192.168.1.198 \
-v E:\yangzhou\SmartDocumentManagementSystem\application.yml:/app/application.yml \
sdms-backend：1.0.0