# 使用与本地开发一致的JDK21基础镜像
FROM eclipse-temurin:21-jdk-jammy

# 设置工作目录
WORKDIR /app

# 拷贝jar包和lib目录
COPY target/sdms-backend-1.0.12.jar app.jar
COPY target/lib ./lib/

# 拷贝配置文件
COPY application.yml ./

# 设置Spring Boot Loader路径，确保app.jar能加载lib下的依赖
ENV LOADER_PATH=./lib/

# 带诊断功能的启动命令
CMD ["sh", "-c", "\
    echo '当前lib目录内容：'; \
    ls -l $LOADER_PATH; \
    echo '启动SDMS应用...'; \
    java -Dloader.path=$LOADER_PATH -jar app.jar \
"]
