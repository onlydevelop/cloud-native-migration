# Order Monolith

This is an order monolith service which is having multiple anti-patterns. We will make this service to a cloud-native app. 

# Project Structure

```
order-monolith/
.
├── Dockerfile
├── README.md
├── mvnw
├── mvnw.cmd
├── pom.xml
├── Makefile
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── example
│   │   │           └── order
│   │   │               ├── OrderMonolithApplication.java
│   │   │               ├── config
│   │   │               │   └── PaymentGatewayProperties.java
│   │   │               ├── controller
│   │   │               │   └── OrderController.java
│   │   │               ├── model
│   │   │               │   ├── InventoryItem.java
│   │   │               │   └── Order.java
│   │   │               ├── repository
│   │   │               │   ├── InventoryRepository.java
│   │   │               │   └── OrderRepository.java
│   │   │               └── service
│   │   │                   ├── InventoryService.java
│   │   │                   ├── NotificationService.java
│   │   │                   ├── OrderService.java
│   │   │                   └── PaymentService.java
│   │   └── resources
│   │       └── application.properties
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
|  4.1  |  Added health check endpoints via Spring Actuator, with liveness/readiness probes enabled and health details hidden from unauthenticated callers  | pom.xml, application.properties  | `1846640`  |
|  5.1  |  Added Resilience4j (`resilience4j-spring-boot3`) and the Spring AOP starter it needs for annotation-based aspects  | pom.xml  | `251730f`  |
|  5.2  |  Configured circuit breaker, retry (exponential backoff), and time limiter instances for the payment gateway, plus a lighter retry-only instance for notifications  | application.properties  | `97f5449`  |
|  5.3  |  Wrapped the payment gateway call with `@CircuitBreaker`/`@Retry`/`@TimeLimiter`; fails closed (declined) on exhausted retries or an open circuit instead of guessing at an unknown outcome  | service/PaymentService.java, service/OrderService.java  | `54a911a`, `850b9bc`  |
|  5.4  |  Made notification sending `@Async` with `@Retry` so a slow/failing SMTP call no longer blocks the order response; `@EnableAsync` added since Spring silently ignores `@Async` without it  | service/NotificationService.java, OrderMonolithApplication.java  | `dea20a6`  |

# Antipatterns

| Sl. No. | Problem | Where | Notes |
|:---------:|:--------|:-------|:-------|
|  1  |  Config hardcoded in properties, no secrets management  | application.properties  | Addressed — values externalized to environment variables (see Changes #1.1, #1.2)  |
|  2  |  Local file DB (H2), not externalized/managed  | application.properties  | Addressed — switched to Postgres (see Changes #2.2)  |
|  3  | In-memory inventory state — won't survive restart, can't scale horizontally   | InventoryService | Addressed — backed by Postgres via JPA with optimistic locking (see Changes #2.1, #2.3)  |
|  4  |  No health check endpoints  | whole app  | Addressed — Spring Actuator health/liveness/readiness probes (see Changes #4.1)  |
|  5  |  Synchronous blocking call to payment gateway, no timeout/retry/circuit breaker  | PaymentService  | Addressed — circuit breaker, retry with backoff, and time limiter via Resilience4j (see Changes #5.1–#5.3)  |
|  6  |  Notification is inline/blocking instead of async/event-driven  | NotificationService, OrderService  | Addressed — `@Async` with retry, off the request's critical path (see Changes #5.4)  |
|  7  | One giant @Transactional method spanning inventory+payment+notification — no compensation/saga pattern   | OrderService.placeOrder  |  |
|  8  |  No structured logging, no correlation IDs, no metrics/tracing  |  whole app |  |
|  9  |  No containerization (no Dockerfile)  | whole repo  | Addressed — multi-stage Dockerfile with non-root user and graceful shutdown (see Changes #3.1–#3.5)  |
|  10  |  No CI/CD, no IaC  |  whole repo |  |
|  11  |  Not idempotent — retrying a failed request double-charges/double-reserves  | OrderService.placeOrder  |  |
|  12  | Single deployable — Order, Inventory, Payment, Notification concerns all coupled in one JAR   |  whole repo |  |
