# 智能业务规则管理平台（Drools + LLM）

基于 **Drools 7.40.0.Final + Spring Boot 2.7.18 + JDK 8 + 智谱AI GLM-4-Flash** 的业务规则演示平台。业务人员用自然语言描述规则，LLM 生成规则 JSON，平台将 JSON 转为 Drools DRL，完成语法校验、冲突检测、沙箱测试，最后保存为规则版本。

## 一、功能概览

内置前端页面 `src/main/resources/static/index.html`（Vue 3 + Element Plus，CDN 引入），启动后访问 http://127.0.0.1:8080 ，左侧菜单共 5 个页面：

| 页面 | 功能 | 调用接口 |
| --- | --- | --- |
| 📦 实体说明 | 展示预置实体 `User` / `Order` / `OrderResult` 及字段 | `GET /api/builtin/entities` |
| 💬 LLM对话生成 | 输入业务描述（如"VIP用户订单满1000打9折"），LLM 返回规则 JSON 草案，可一键应用到设计器 | `POST /api/llm/generate` |
| 📐 规则设计器 | 三种视图：业务流程视图 / JSON 源码（可编辑后应用）/ DRL 预览；支持语法校验、冲突检测、命名规范检查 | `POST /api/rule/validate` |
| 🧪 测试验证 | 用自然语言描述测试场景，LLM 生成测试事实 JSON；在沙箱中执行规则，展示命中规则数和执行前后的事实快照；命中后可直接发布 | `POST /api/llm/testcase/generate`、`POST /api/testcase/run-direct`、`POST /api/rule/save-direct` |
| ✅ 审核发布 | 填写版本备注后保存规则 JSON 与 DRL 到数据库 | `POST /api/rule/save-direct` |

## 二、技术栈

| 层级 | 选型 |
| --- | --- |
| 基础 | JDK 1.8、Spring Boot 2.7.18、Maven |
| 规则引擎 | Drools 7.40.0.Final（drools-core / drools-compiler / kie-spring） |
| 持久层 | MyBatis-Plus 3.5.5、MySQL 5.7+ / 8.0（mysql-connector-java 8.0.33） |
| LLM | 智谱AI开放平台 v4 `chat/completions`，默认模型 `glm-4-flash`，`temperature=0.1` |
| 前端 | Vue 3 + Element Plus 单页（unpkg CDN，浏览器需能访问外网） |
| 工具 | Hutool 5.8.25、commons-lang3、Jackson、Lombok |

## 三、目录结构

```
droolsBookAIDemo/
├── pom.xml
├── sql/
│   └── init.sql                         # 完整版 6 张表（含实体/测试用例/审核表），需手动执行
└── src/main/
    ├── java/com/biz/rule/
    │   ├── RuleApplication.java          # 启动类
    │   ├── common/                       # Result 统一返回、BizException、全局异常处理
    │   ├── config/WebConfig.java         # /api/** 跨域配置
    │   ├── entity/
    │   │   ├── User / Order / OrderResult  # 预置业务实体（规则中直接 import）
    │   │   └── *DO.java                  # 数据表映射
    │   ├── mapper/                       # MyBatis-Plus Mapper
    │   ├── engine/
    │   │   ├── model/                    # RuleModel（规则JSON模型）、EntityModel（实体定义模型）
    │   │   ├── convert/JsonToDrlConverter   # 规则JSON → DRL
    │   │   ├── check/ConflictDetector       # 冲突与重复检测
    │   │   ├── sandbox/SandboxExecutor      # 事实JSON → Java对象 → fireAllRules
    │   │   ├── compiler/DynamicEntityCompiler  # 由实体定义生成 Java 源码
    │   │   └── DroolsEngineHelper        # KieFileSystem/KieBuilder 动态构建 KieContainer
    │   ├── llm/
    │   │   ├── PromptBuilder             # 规则生成的系统提示词（约束输出 JSON 结构）
    │   │   └── ZhipuLlmClient            # 智谱 API 调用（RestTemplate）
    │   ├── service/                      # Rule / Validate / Llm / LlmTestCase / Entity / TestCase / Audit
    │   └── controller/                   # REST 接口
    └── resources/
        ├── application.yml
        ├── schema.sql                    # 启动时自动执行：rule_info / rule_version / llm_config
        ├── data.sql                      # 启动时自动执行：插入默认 LLM 配置 zhipu_default
        └── static/index.html             # 前端页面
```

## 四、快速启动

### 1. 准备数据库

```sql
CREATE DATABASE IF NOT EXISTS drools_ai_platform DEFAULT CHARACTER SET utf8mb4;
```

应用启动时会自动执行 `schema.sql` 和 `data.sql`，创建 `rule_info`、`rule_version`、`llm_config` 三张表。前端主流程只用到这三张表。

如果还要使用实体管理、测试用例、审核流程这几组接口（见第六节），需要先执行一次 `sql/init.sql` 创建 `rule_entity`、`rule_test_case`、`rule_audit` 表：

```bash
mysql -uroot -p < sql/init.sql
```

### 2. 配置环境变量

`application.yml` 中不保存任何真实账号和密钥，敏感信息全部通过环境变量注入：

| 环境变量 | 对应配置 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `DB_HOST` | datasource.url | `127.0.0.1` | MySQL 地址 |
| `DB_PORT` | datasource.url | `3306` | MySQL 端口 |
| `DB_NAME` | datasource.url | `drools_ai_platform` | 数据库名 |
| `DB_USERNAME` | datasource.username | `root` | 数据库用户 |
| `DB_PASSWORD` | datasource.password | 空 | 数据库密码 |
| `LLM_API_KEY` | llm.api-key | 空 | 智谱 API Key，在 https://open.bigmodel.cn 控制台 → API Keys 获取 |
| `SQL_INIT_MODE` | spring.sql.init.mode | `always` | 设为 `never` 可停止启动时重建表 |

PowerShell 示例：

```powershell
$env:DB_HOST="127.0.0.1"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的数据库密码"
$env:LLM_API_KEY="你的智谱API Key"
```

Linux / macOS：

```bash
export DB_HOST=127.0.0.1 DB_USERNAME=root DB_PASSWORD='你的数据库密码' LLM_API_KEY='你的智谱API Key'
```

LLM 密钥的取值规则：`LlmService` 先读取 `llm_config` 表中 `config_code='zhipu_default'` 且启用的记录；如果该记录的 `api_key` 为空或为占位符 `YOUR_ZHIPU_API_KEY`（`data.sql` 默认写入的值），则使用环境变量 `LLM_API_KEY`。两者都没有时接口返回 "LLM API Key 未配置"。

### 3. 编译运行

```bash
mvn clean package -DskipTests
java -jar target/drools-ai-platform.jar
```

也可以在 IDE 中直接运行 `com.biz.rule.RuleApplication`。启动后访问 http://127.0.0.1:8080 。

> ⚠️ `schema.sql` 包含 `DROP TABLE`，在默认的 `SQL_INIT_MODE=always` 下，**每次启动都会清空 `rule_info`、`rule_version`、`llm_config` 中的数据**。需要保留已发布的规则时，首次启动后改为 `SQL_INIT_MODE=never`。

## 五、核心数据结构

### 1. 预置实体

规则只能引用以下 3 个类（LLM 提示词中也只声明了这 3 个）：

| 类 | 字段 |
| --- | --- |
| `com.biz.rule.entity.User` | `id`(Long)、`name`(String)、`age`(Integer)、`userLevel`(VIP/NORMAL)、`status`(NORMAL/FROZEN) |
| `com.biz.rule.entity.Order` | `id`(Long)、`userId`(Long)、`amount`(BigDecimal)、`finalAmount`(BigDecimal)、`status`(NEW/PAID/CANCELLED) |
| `com.biz.rule.entity.OrderResult` | `code`(Integer)、`content`(String) |

### 2. 规则 JSON（`RuleModel`）

`when` / `then` 直接存放 DRL 原文字符串，多行用 `\n` 分隔，以保留绑定变量、跨事实引用、`modify` 等完整语法：

```json
{
  "package": "rules.marketing",
  "imports": ["com.biz.rule.entity.User", "com.biz.rule.entity.Order",
              "com.biz.rule.entity.OrderResult", "java.math.BigDecimal"],
  "rules": [
    {
      "name": "vip_10_percent_off",
      "salience": 10,
      "enabled": true,
      "noLoop": false,
      "when": "$user: User(userLevel == \"VIP\", status == \"NORMAL\")\n$order: Order(userId == $user.id, amount.compareTo(new BigDecimal(\"1000\")) >= 0)\n$ord: OrderResult()",
      "then": "$order.setFinalAmount($order.getAmount().multiply(new BigDecimal(\"0.9\")));\nupdate($order);\nmodify($ord){ setCode(200), setContent(\"VIP订单满1000打9折\") }",
      "comment": "VIP用户订单满1000享9折"
    }
  ]
}
```

还支持 `globals`、`functions`、`queries` 字段，`JsonToDrlConverter` 按 package → import → global → function → query → rule 的顺序输出 DRL。`package` 字段也兼容写作 `packageName`。

### 3. 测试事实 JSON

每个事实必须带 `$entityClass` 指定全类名，沙箱据此从 KieContainer 的 ClassLoader 加载类并用 Jackson 转换；`BigDecimal` 字段用字符串表示：

```json
[
  {"$entityClass": "com.biz.rule.entity.User", "id": 10001, "name": "来家二少", "age": 30, "userLevel": "VIP", "status": "NORMAL"},
  {"$entityClass": "com.biz.rule.entity.Order", "id": 1, "userId": 10001, "amount": "1300.00", "status": "NEW"},
  {"$entityClass": "com.biz.rule.entity.OrderResult", "code": 0, "content": ""}
]
```

## 六、REST 接口

所有接口统一返回 `{"code": 200, "message": "success", "data": ...}`，业务异常时 `code` 为 500（或 `BizException` 指定的值）。

### 1. 前端主流程接口（只依赖 schema.sql 的 3 张表）

| 方法 | 路径 | 请求体 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/builtin/entities` | - | 预置实体及字段说明 |
| POST | `/api/llm/generate` | `{"businessDesc": "...", "configCode": "zhipu_default"}` | 业务描述 → 规则 JSON 字符串（自动去除 ```json 包裹） |
| POST | `/api/llm/testcase/generate` | `{"scenario": "..."}` | 测试场景 → 事实 JSON 数组 |
| POST | `/api/rule/validate` | 规则 JSON 原文 | 返回 `drl`、`syntaxOk`、`syntaxError`、`conflicts`、`conflictCount`、`namingIssues` |
| POST | `/api/rule/preview-drl` | 规则 JSON 对象 | 仅返回转换后的 DRL |
| POST | `/api/testcase/run-direct` | `{"ruleJson": "...", "facts": [...]}` | 沙箱执行，返回 `firedRules`、`beforeSnapshot`、`afterSnapshot` |
| POST | `/api/rule/save-direct` | `{"ruleJson": "...", "remark": "..."}` | 以第一条规则名作为 `rule_code`，不存在则新建（状态=已发布），存在则版本号 +1，写入 JSON 与 DRL |

### 2. 扩展接口（需要先执行 `sql/init.sql`）

| 模块 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 规则 | GET | `/api/rule/list` | 规则列表 |
| 规则 | GET | `/api/rule/{id}` | 规则详情 |
| 规则 | POST | `/api/rule/create` | 创建规则（状态=草稿，版本=0） |
| 规则 | POST | `/api/rule/{id}/version` | 保存新版本：`{ruleJson, entityRefs, user, remark}`，JSON→DRL 并做冲突检测 |
| 规则 | GET | `/api/rule/{id}/versions` | 版本列表 |
| 规则 | GET | `/api/rule/{id}/version/{version}` | 指定版本 |
| 实体 | GET | `/api/entity/list` | 自定义实体列表 |
| 实体 | GET | `/api/entity/{id}` | 实体详情 |
| 实体 | POST | `/api/entity/save` | 新增/修改实体（`field_json` 为 `EntityModel` JSON） |
| 实体 | POST | `/api/entity/source/{entityCode}` | 由实体定义生成 Java 源码 |
| 实体 | DELETE | `/api/entity/{id}` | 删除实体 |
| 测试 | GET | `/api/testcase/list/{ruleId}` | 用例列表 |
| 测试 | POST | `/api/testcase/save` | 保存用例 |
| 测试 | POST | `/api/testcase/execute/{id}` | 执行用例：按规则 imports 加载自定义实体源码 → 沙箱执行 → 回写实际结果和通过状态 |
| 测试 | DELETE | `/api/testcase/{id}` | 删除用例 |
| 审核 | POST | `/api/audit/submit` | 提交审核 `{ruleId, version, submitUser}`，规则状态 → 审核中 |
| 审核 | POST | `/api/audit/{id}` | 审核 `{auditUser, pass, opinion}`，通过 → 已发布，驳回 → 草稿 |
| 审核 | GET | `/api/audit/pending` | 待审核列表 |

规则状态：`0` 草稿、`1` 审核中、`2` 已发布、`3` 已停用。

## 七、核心处理流程

```
业务自然语言
    │  POST /api/llm/generate（PromptBuilder 系统提示词 + 智谱 GLM）
    ▼
规则 JSON（RuleModel）
    │  POST /api/rule/validate
    ├─ JsonToDrlConverter → DRL
    ├─ DroolsEngineHelper：KieFileSystem 写入 DRL → KieBuilder.buildAll() 检查编译错误
    ├─ ConflictDetector：规则名重复 / 条件完全相同 / salience 相同且条件重叠 ≥ 60%
    └─ 命名规范：规则名需匹配 ^[a-z][a-z0-9_]*$
    ▼
测试事实 JSON（POST /api/llm/testcase/generate 生成，或手写）
    │  POST /api/testcase/run-direct
    ▼
SandboxExecutor：构建 KieContainer → 按 $entityClass 实例化事实 → insert → fireAllRules
    │  返回命中规则数 + 执行前后事实快照
    ▼
POST /api/rule/save-direct → rule_info + rule_version（JSON 与 DRL 一并入库）
```

## 八、当前限制

- LLM 提示词固定声明了 `User` / `Order` / `OrderResult` 三个实体，`/api/llm/generate` 请求中的 `packageName`、`entities` 参数目前未被使用。
- `/api/rule/validate` 和 `/api/testcase/run-direct` 只识别 classpath 中的预置实体；只有 `/api/testcase/execute/{id}` 会从 `rule_entity` 表加载自定义实体源码参与编译。
- `application.yml` 中 `llm` 段只有 `api-key` 被代码读取；接口地址、模型名以 `llm_config` 表为准。`rule-platform` 段（沙箱超时、最大触发数、实体缓存）尚未接入代码，沙箱执行没有超时和触发次数限制。
- 每次校验或执行都会新建一个 KieContainer，没有缓存。
- 测试用例的通过判定是检查执行后快照是否包含 `expect_result` 字符串。
- 跨域配置对 `/api/**` 放开所有来源，仅适合本地演示；接口没有登录鉴权。
- `sql/init.sql` 与 `schema.sql` 中 `rule_info`、`rule_version`、`llm_config` 的表结构略有差异（如 `rule_info.status` 默认值），启动后以 `schema.sql` 为准。
