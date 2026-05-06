# HotPulse AI

基于 Spring Boot（Java）与前端 React + Vite（TypeScript）的热点监测与智能助手平台。

主要功能
- 监控关键词并生成热点列表（Hotspots）
- 与智能 agent 聊天并查看会话历史（SSE 实时推送）
- 生成每日报告（模板驱动）
- 文档标注、事实核验等批量/单条处理能力
- 后端作业与 Flyway 数据库迁移支持

仓库结构（精要）
- `src/main/java/`：后端 Spring Boot 源代码
- `src/main/resources/`：后端配置与 SQL 迁移脚本（`db/migration`）和 prompt 模板（`prompts/`）
- `frontend/`：前端源码（Vite + React + TypeScript）
- `pom.xml`：Maven 构建文件
- `docker-compose.yml`、`Dockerfile`：容器与编排配置

快速开始（开发）

1) 启动后端（开发模式）

```bash
# 在仓库根目录
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

或打包并运行：

```bash
mvn clean package -DskipTests
java -jar target/*.jar --spring.profiles.active=prod
```

2) 启动前端（开发模式）

```bash
cd frontend
pnpm install
pnpm run dev
# 访问 http://localhost:5173 （默认）
```

3) 使用 Docker（一键）

```bash
docker-compose up --build
```

配置说明
- 后端配置文件位于 `src/main/resources/application.yml` 及环境覆盖文件（如 `application-dev.yml`、`application-prod.yml`），请根据需要修改数据库连接、API Key 等配置。
- 前端使用 Vite 环境变量（以 `VITE_` 前缀）。在 `frontend/` 下创建 `.env.local`，例如：

```
VITE_API_BASE_URL=http://localhost:8080
```

数据库迁移
- 项目包含 Flyway 迁移脚本，位于 `src/main/resources/db/migration`。应用程序启动时会自动应用迁移。

常用命令汇总
- 启动后端（开发）：`mvn spring-boot:run -Dspring-boot.run.profiles=dev`
- 打包后端：`mvn clean package -DskipTests`
- 启动前端：`pnpm install && pnpm run dev`（在 `frontend/`）
- 构建前端：`pnpm run build`
- Docker：`docker-compose up --build`

开发建议
- 本地开发时建议同时启动后端和前端，并在前端通过 `VITE_API_BASE_URL` 指向后端。
- 后端使用 Spring Profiles 管理不同环境的配置（`dev`/`prod`）。
- 代码风格与提交信息请遵循团队约定。

贡献与联系方式
- 欢迎提交 Issue/PR。请在 PR 描述中包含变更目的与测试步骤。

许可证
- 请在将仓库发布到 GitHub 前补充许可证文件（`LICENSE`）。

更多帮助
- 如果你希望我把 README 翻成英文、添加徽章（CI、license、build status）、或根据具体部署（如 Kubernetes、Cloud）扩展运行文档，我可以继续补充。
