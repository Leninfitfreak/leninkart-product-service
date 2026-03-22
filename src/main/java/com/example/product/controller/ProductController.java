package com.example.product.controller;

import com.example.product.model.Product;
import com.example.product.observability.BusinessMetrics;
import com.example.product.observability.StructuredLog;
import com.example.product.repo.ProductRepository;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductRepository repo;
    private final KafkaTemplate<String, String> kafka;
    private final BusinessMetrics metrics;

    public ProductController(ProductRepository repo, KafkaTemplate<String, String> kafka, BusinessMetrics metrics) {
        this.repo = repo;
        this.kafka = kafka;
        this.metrics = metrics;
    }

    @GetMapping
    public List<Product> all(@RequestAttribute("userId") String userId,
                             @RequestAttribute(value = "role", required = false) String role) {
        if (userId == null || userId.isBlank()) {
            return List.of();
        }
        if ("ADMIN".equalsIgnoreCase(role)) {
            return repo.findAllByOrderByIdDesc();
        }
        return repo.findAllByCreatedByOrderByIdDesc(userId);
    }

    @PostMapping
    public Product create(@RequestBody Product p, @RequestAttribute("userId") String userId) {
        String owner = (userId == null || userId.isBlank()) ? "anonymous" : userId.trim();
        p.setCreatedBy(owner);
        Product saved = repo.save(p);
        metrics.recordProductCreated();
        StructuredLog.info(logger, "http.product.create", "Product created", Map.of(
            "product_id", saved.getId(),
            "created_by", owner
        ));
        return saved;
    }

    @PostMapping("/{id}/order")
    public ResponseEntity<String> order(@PathVariable Long id, @RequestAttribute("userId") String userId) {
        Optional<Product> op = repo.findById(id);
        if (op.isEmpty()) {
            StructuredLog.info(logger, "http.product.order", "Product not found for order request", Map.of(
                "product_id", id
            ));
            return ResponseEntity.notFound().build();
        }

        Product p = op.get();
        String who = (userId == null || userId.isBlank()) ? "anonymous" : userId.trim();
        metrics.recordOrderRequest();
        Timer.Sample publishTimer = metrics.startOrderRequestPublish();
        String payload = String.format(
            "{\"productId\":%d,\"name\":\"%s\",\"price\":%s,\"user\":\"%s\"}",
            p.getId(),
            p.getName(),
            p.getPrice(),
            who
        );
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("topic", "product-orders");
        fields.put("product_id", p.getId());
        fields.put("user", who);
        fields.put("operation_kind", "kafka_producer");
        StructuredLog.info(logger, "kafka.produce.product-orders", "Publishing order request", fields);

        kafka.send("product-orders", payload).whenComplete((result, error) -> {
            metrics.stopOrderRequestPublish(publishTimer);
            if (error != null) {
                metrics.recordOrderRequestPublishFailure();
                StructuredLog.error(logger, "kafka.produce.product-orders", "Failed to publish order request", error, fields);
                return;
            }
            metrics.recordOrderRequestPublishSuccess();
            Map<String, Object> successFields = new LinkedHashMap<>(fields);
            if (result != null && result.getRecordMetadata() != null) {
                successFields.put("partition", result.getRecordMetadata().partition());
                successFields.put("offset", result.getRecordMetadata().offset());
            }
            StructuredLog.info(logger, "kafka.produce.product-orders", "Order request published", successFields);
        });
        return ResponseEntity.ok("order-sent");
    }
}
