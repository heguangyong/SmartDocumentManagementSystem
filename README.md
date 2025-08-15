# 📄 Smart Document Management System (SDMS) Backend
docker build -t sdms-backend .
docker network create my-network
docker run -d --name sdms-backend -p 8080:8080 --network my-network sdms-backend