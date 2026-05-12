# ESL PanPan Protocol Consumer

攀攀电子价签 MVP 协议消费者服务。业务依据为《第一版 MVP 的生产者与消费者设计.md》。

本服务只负责消费者侧协议执行：

- 消费 RabbitMQ 指令任务。
- 按 `messageType` 转换为攀攀 MQTT topic/payload。
- 发布到 EMQX。
- 更新 `command_task` 状态。
- 订阅 AP/ESL 上报，处理 AP 心跳、运行状态、ESL 状态、AP ACK、reqkey。
- reqkey 生成内部 `TAG_KEY_REPLY` 任务并重新投递 RabbitMQ。
- 记录 `command_event_log` 和关键 MQTT 原始 payload。

不实现后台商品价格修改、模板管理、AP 创建、正式业务生产 API，也不替代生产者仓库。

## 技术栈

- Java 17+
- Spring Boot 3.x
- Maven
- Spring AMQP
- Spring Data JPA
- MySQL 8
- Flyway
- Jackson
- Lombok
- Validation
- Eclipse Paho MQTT
- JUnit 5 / Mockito / Testcontainers
- Docker Compose

## 消息契约

RabbitMQ:

| 用途 | exchange | queue | routingKey |
| --- | --- | --- | --- |
| command | `esl.command.exchange` | `panpan.command.queue` | `panpan.command` |
| report | `esl.report.exchange` | `panpan.report.queue` | `panpan.report` |
| dead | `esl.dead.exchange` | `panpan.dead.queue` | fanout |

MQTT:

| 方向 | Topic |
| --- | --- |
| 发布 AP 管理 | `esl/server/mgr/{ap}` |
| 发布门店数据 | `esl/server/data/{shop}` |
| 订阅 AP report | `esl/ap/report/+/+` |
| 订阅 AP ACK | `esl/ap/ack/+` |
| 订阅 reqkey | `esl/esl/reqkey/+` |

支持任务类型：

- `AP_BIND_SHOP`
- `AP_TIME_SYNC`
- `TEMPLATE_PUBLISH`
- `FONT_PUBLISH`
- `TAG_UPDATE`
- `TAG_KEY_REPLY`

任务状态：

`CREATED`, `QUEUED`, `PUBLISHED`, `AP_ACKED`, `ESL_REPORTED`, `SUCCESS`, `FAILED`, `TIMEOUT`

说明：AP ACK 只代表 AP 收到任务，不代表价签刷新成功。`report/tag` 的 `stat` 第一版只保存原始状态码。

## 启动

```bash
docker compose up -d
./mvnw spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/actuator/health
```

RabbitMQ Management:

- URL: http://localhost:15673
- username/password: `panpan` / `panpan`

EMQX Dashboard:

- URL: http://localhost:18083
- username/password: `admin` / `public`

MySQL:

- JDBC: `jdbc:mysql://localhost:3307/esl_panpan`
- username/password: `panpan` / `panpan`

默认宿主端口避开常见本地服务冲突：MySQL `3307`，RabbitMQ AMQP `5673`，RabbitMQ Management `15673`，MQTT `1883`。需要恢复标准端口时可在启动前设置 `PANPAN_MYSQL_PORT=3306 PANPAN_RABBITMQ_PORT=5672 PANPAN_RABBITMQ_MANAGEMENT_PORT=15672`。

## 测试

```bash
./mvnw clean test
```

集成测试使用 Testcontainers 启动 MySQL 和 RabbitMQ，并 mock `MqttPublisher`，覆盖：

- 协议转换
- `wtag` 构造
- RabbitMQ 指令消费
- MQTT 发布 mock
- ACK 状态处理
- reqkey 生成 `TAG_KEY_REPLY`
- ESL report 状态更新
- timeout 扫描
- 非法 JSON 死信

## 手动联调示例

投递 `TAG_UPDATE`：

```bash
TASK_UUID="$(uuidgen)"
curl -u panpan:panpan -H 'content-type: application/json' \
  -X POST http://localhost:15673/api/exchanges/%2f/esl.command.exchange/publish \
  -d "{
    \"properties\": {\"content_type\": \"application/json\"},
    \"routing_key\": \"panpan.command\",
    \"payload_encoding\": \"string\",
    \"payload\": \"{\\\"messageType\\\":\\\"TAG_UPDATE\\\",\\\"brand\\\":\\\"PANPAN\\\",\\\"shopCode\\\":\\\"ZH01\\\",\\\"apCode\\\":\\\"ESLAP00000008\\\",\\\"tagId\\\":\\\"6597069770841\\\",\\\"templateName\\\":\\\"PRICEPROMO\\\",\\\"screenCode\\\":\\\"06\\\",\\\"modelDecimal\\\":6,\\\"forceRefresh\\\":1,\\\"product\\\":{\\\"productName\\\":\\\"脉动 维生素饮料青柠口味 600ML\\\",\\\"productCode\\\":\\\"6902538004045\\\",\\\"price\\\":\\\"10.80\\\",\\\"spec\\\":\\\"600ML\\\",\\\"qrContent\\\":\\\"esl.wdyc.cn\\\",\\\"promoPrice\\\":null},\\\"taskUuid\\\":\\\"${TASK_UUID}\\\",\\\"vendorTaskId\\\":39138}\"
  }"
```

查询任务：

```bash
docker compose exec mysql mysql -upanpan -ppanpan esl_panpan \
  -e "select task_uuid,message_type,status,mqtt_topic from command_task order by id desc limit 5;"
```

模拟 AP ACK：

```bash
docker run --rm --network host eclipse-mosquitto:2 \
  mosquitto_pub -h localhost -p 1883 \
  -t "esl/ap/ack/ESLAP00000008" \
  -m "{\"id\":\"${TASK_UUID}\",\"code\":0}"
```

模拟 ESL report/tag：

```bash
docker run --rm --network host eclipse-mosquitto:2 \
  mosquitto_pub -h localhost -p 1883 \
  -t "esl/ap/report/tag/ESLAP00000008" \
  -m "{\"tag\":\"6597069770841\",\"ssirs\":-42,\"batterysoc\":92,\"tempt\":23.5,\"ap\":\"ESLAP00000008\",\"shop\":\"ZH01\",\"stat\":4}"
```

## 协议扩展点

`checksum` 和 `token` 的真实算法资料不足，当前封装为可替换接口：

- `ChecksumCalculator`
- `TokenProvider`

默认实现基于 SHA-256，保证本地可运行，真实算法补齐后只需替换接口实现，不需要改业务消费者。
