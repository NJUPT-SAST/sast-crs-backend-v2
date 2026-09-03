# SAST-CRS-v2

通用评审系统（Common Review System）后端服务，由 NJUPT-SAST 维护。提供赛事创建、团队报名、作品提交、评委分配、在线评审、成绩管理与 Excel 导出等完整评审流程。

v2 为开源重构版：所有敏感配置改为环境变量注入，不包含任何部署凭据。

## 技术栈

- Java 21 / Spring Boot 4.1
- MyBatis-Plus 3.5 / MySQL 5.7+（或 MariaDB）
- Redis（缓存与分布式锁）
- JWT + 验证码登录
- 腾讯云 COS（文件预签名上传/下载）
- EasyExcel（成绩与作品导出）

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- MySQL 5.7+ 与 Redis（本机或远端）

### 初始化数据库

```sql
SOURCE src/main/resources/schema.sql;  -- 建库建表
SOURCE src/main/resources/data.sql;    -- 可选：院系数据与演示种子账号
```

`data.sql` 中的种子账号仅用于本地开发，密码统一为 **`sastSu`**（MD5 存储），请勿在生产环境使用。

### 配置

复制 `.env` 模板并填入自己的信息（**直跑与 docker compose 两种启动方式共用这一份配置**，`.env` 已加入 .gitignore）：

```bash
cp -n .env.example .env
vim .env
```

`.env` 中的变量（均有内置默认值，按需覆盖）：

| 环境变量 | 说明 |
| --- | --- |
| `JWT_SECRET` | JWT 签名密钥（**必填**，任意随机字符串） |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | MySQL 连接信息 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` / `REDIS_DATABASE` | Redis 连接信息 |
| `COS_SECRET_ID` / `COS_SECRET_KEY` / `COS_BUCKET_NAME` / `COS_REGION` | 腾讯云 COS 凭证与桶信息 |
| `COS_PUBLIC_FOLDER` / `COS_PRIVATE_FOLDER` | COS 公共/私有文件夹名 |
| `APP_DEFAULT_COVER` | 作品默认封面 URL |
| `SERVER_PORT` | 服务端口（默认 1080，与前端代理一致） |

### 运行

本地直跑（使用默认配置，端口 1080）：

```bash
mvn spring-boot:run
```

Docker 部署（开发/生产统一，端口 1080，自动拉起 MySQL + Redis 并初始化数据库）：

```bash
docker compose up --build -d     # 构建并启动 mysql + redis + app
docker compose logs -f app       # 查看日志
docker compose down -v           # 停止并删除数据
```

开发便利：MySQL 映射到宿主机 `3307`、Redis 映射到 `6380`，可用图形客户端直连。

需要修改部署配置时，复制 `.env.example` 为 `.env`（已加入 .gitignore），按注释填写后重新 `docker compose up -d` 生效：`JWT_SECRET`、`MYSQL_ROOT_PASSWORD`、`COS_SECRET_ID` 等。注意 `MYSQL_ROOT_PASSWORD` 仅在首次创建 mysql 数据卷时生效。

## 项目结构

```
src/main/java/com/sast/crs/
├── controller/   # 接口层（登录、用户、管理员、评审、成绩、通用接口）
├── service/      # 业务逻辑层
├── mapper/       # MyBatis-Plus 数据访问层
├── entity/       # 数据库实体
├── model/        # 视图模型（VO）
├── config/       # 配置类（JWT 拦截器、MyBatis-Plus、COS、调度任务等）
├── interceptor/  # 请求拦截（鉴权、日志 MDC）
└── util/         # 工具类（JWT、COS、Excel 等）
```

## 数据库表

`department` 院系、`user` 用户、`competition` 赛事、`team` 团队、`work` 作品、`file` 文件、`judge` 评委、`review` 评审、`score` 成绩、`notice` 公告、`white_list` 白名单。

## 与 v1 的主要区别

本仓库为 [SAST-CRS-Backend](https://github.com/NJUPT-SAST/SAST-CRS-Backend)（v1）的开源重构版，主要变更：

| 组件 | v1 | v2（本仓库） |
| --- | --- | --- |
| Java | 17 | **21**（LTS） |
| Spring Boot | 2.7.1（已 EOL） | **4.1**（Jakarta EE，`javax.*` → `jakarta.*`） |
| MyBatis-Plus | 3.5.2（boot-starter） | **3.5.17**（spring-boot4-starter；IService 迁至 `spring.service` 包，分页插件拆分至 jsqlparser 模块） |
| MySQL 驱动 | `mysql-connector-java`（旧坐标） | `com.mysql:mysql-connector-j`（版本由 Boot 管理） |
| JWT | java-jwt 4.2.1 | 4.5.2 |
| JSON | fastjson2 2.0.18 | 2.0.64 |
| JSON 序列化 | Jackson 2（`com.fasterxml`） | **Jackson 3**（`tools.jackson`，注解包不变） |
| Excel 导出 | EasyExcel 3.1.2 | 4.0.3 |
| 工具库 | hutool-http 5.8.9 | 5.8.47 |
| 验证码 | penggle kaptcha 2.3.2（依赖 javax，无法运行） | hutool-captcha |
| 对象存储 | 腾讯云 COS 5.6.210 | 5.6.275（唯一存储，清除 MinIO/OSS 遗留命名） |
| 日志配置 | logback 按环境分支（`springProfile`） | 全环境统一（Boot 4 已移除 `springProfile` 扩展） |
| 配置管理 | 密码/密钥散落在配置文件中 | 全部环境变量占位：公共配置一份 + 单一 local 模板 |
| 部署 | GitHub Actions SSH 自动部署 + Docker | docker compose 统一开发与部署，CI 仅做构建检查 |
| 种子数据 | 真实 MD5 密码哈希 | 演示密码 `sastSu` |

升级原因：v1 核心依赖（Spring Boot 2.7）已停止维护，且历史提交中混有真实凭据；v2 以全新仓库发布，敏感信息全部外置，便于开源维护。

## 许可证

[MIT](LICENSE)
