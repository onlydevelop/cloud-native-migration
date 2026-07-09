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

# Antipatterns

| Sl. No. | Problem | Where |
|:---------:|:--------|:-------|
|  1  |  Config hardcoded in properties, no secrets management  | application.properties  |
|  2  |  Local file DB (H2), not externalized/managed  | application.properties  |
|  3  | In-memory inventory state — won't survive restart, can't scale horizontally   | InventoryService |
|  4  |  No health check endpoints  | whole app  |
|  5  |  Synchronous blocking call to payment gateway, no timeout/retry/circuit breaker  | PaymentService  |
|  6  |  Notification is inline/blocking instead of async/event-driven  | NotificationService, OrderService  |
|  7  | One giant @Transactional method spanning inventory+payment+notification — no compensation/saga pattern   | OrderService.placeOrder  |
|  8  |  No structured logging, no correlation IDs, no metrics/tracing  |  whole app |
|  9  |  No containerization (no Dockerfile)  | whole repo  |
|  10  |  No CI/CD, no IaC  |  whole repo |
|  11  |  Not idempotent — retrying a failed request double-charges/double-reserves  | OrderService.placeOrder  |
|  12  | Single deployable — Order, Inventory, Payment, Notification concerns all coupled in one JAR   |  whole repo |
