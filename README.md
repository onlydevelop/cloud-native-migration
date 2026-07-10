# Order Monolith

This is an order monolith service which is having multiple anti-patterns. We will make this service to a cloud-native app. 

# Project Structure

```
order-monolith/
├── pom.xml
├── src/main/java/com/example/order/
│   ├── OrderMonolithApplication.java
│   ├── model/Order.java
│   ├── controller/OrderController.java
│   ├── repository/OrderRepository.java
│   ├── service/OrderService.java
│   ├── service/InventoryService.java
│   ├── service/PaymentService.java
│   ├── service/NotificationService.java
└── src/main/resources/application.properties
```

# Changes

| Sl. No. | Change | Where | Commit |
|:---------:|:--------|:-------|:-------|
|  1.1  |  Externalized hardcoded config (datasource, payment gateway, notification, invoice storage) to environment variables, with prior local values kept as defaults  | application.properties  | `0dde672`  |
|  1.2  |  Added type-safe `PaymentGatewayProperties` bound to the `payment.gateway.*` prefix instead of scattered `@Value` lookups  | config/PaymentGatewayProperties.java  | `ea49eb1`  |
|  2.1  |  Made inventory stateless by backing it with a JPA `InventoryItem` entity (optimistic locking via `@Version`) instead of an in-memory map, so reservations are consistent across instances  | model/InventoryItem.java, repository/InventoryRepository.java, service/InventoryService.java  | `1402566`  |
|  2.2  |  Switched datasource from local-file H2 to Postgres, since state must be shared across instances  | pom.xml, application.properties  | `46c3f9d`  |
|  2.3  |  Mapped `Order` entity to table `orders` to avoid a reserved-keyword clash with Postgres (`ORDER`), which H2 had tolerated  | model/Order.java  | `0790a12`  |
|  3.1  |  Added a multi-stage Dockerfile (cached dependency layer, non-root runtime user)  | Dockerfile  | `b0084b0`  |
|  3.2  |  Enabled graceful shutdown so SIGTERM drains in-flight requests instead of killing them immediately  | application.properties  | `b119a47`  |
|  3.3  |  Added `.dockerignore` to keep build output, IDE files, and old H2 data out of the build context  | .dockerignore  | `509f093`  |
|  3.4  |  Added the Maven wrapper (`mvnw`, `.mvn/`) so the Docker build doesn't depend on a local Maven install  | mvnw, mvnw.cmd, .mvn/  | `dbea32b`  |
|  3.5  |  Fixed the Dockerfile's jar path to match the actual build artifact (`order-monolith-0.0.1-SNAPSHOT.jar`, not the hardcoded `1.0.0`) via a wildcard  | Dockerfile  | `902fce4`  |

# Antipatterns

| Sl. No. | Problem | Where | Notes |
|:---------:|:--------|:-------|:-------|
|  1  |  Config hardcoded in properties, no secrets management  | application.properties  | Addressed — values externalized to environment variables (see Changes #1.1, #1.2)  |
|  2  |  Local file DB (H2), not externalized/managed  | application.properties  | Addressed — switched to Postgres (see Changes #2.2)  |
|  3  | In-memory inventory state — won't survive restart, can't scale horizontally   | InventoryService | Addressed — backed by Postgres via JPA with optimistic locking (see Changes #2.1, #2.3)  |
|  4  |  No health check endpoints  | whole app  |  |
|  5  |  Synchronous blocking call to payment gateway, no timeout/retry/circuit breaker  | PaymentService  |  |
|  6  |  Notification is inline/blocking instead of async/event-driven  | NotificationService, OrderService  |  |
|  7  | One giant @Transactional method spanning inventory+payment+notification — no compensation/saga pattern   | OrderService.placeOrder  |  |
|  8  |  No structured logging, no correlation IDs, no metrics/tracing  |  whole app |  |
|  9  |  No containerization (no Dockerfile)  | whole repo  | Addressed — multi-stage Dockerfile with non-root user and graceful shutdown (see Changes #3.1–#3.5)  |
|  10  |  No CI/CD, no IaC  |  whole repo |  |
|  11  |  Not idempotent — retrying a failed request double-charges/double-reserves  | OrderService.placeOrder  |  |
|  12  | Single deployable — Order, Inventory, Payment, Notification concerns all coupled in one JAR   |  whole repo |  |
