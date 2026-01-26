package com.example.product.controller;
import com.example.product.model.Product; import com.example.product.repo.ProductRepository; import org.springframework.http.ResponseEntity; import org.springframework.kafka.core.KafkaTemplate; import org.springframework.web.bind.annotation.*; import java.util.List; import java.util.Optional;
@RestController @RequestMapping("/api/products") public class ProductController {
 private final ProductRepository repo; private final KafkaTemplate<String,String> kafka;
 public ProductController(ProductRepository repo,KafkaTemplate<String,String> kafka){this.repo=repo;this.kafka=kafka;}
 @GetMapping public List<Product> all(){return repo.findAll();}
 @PostMapping public Product create(@RequestBody Product p){return repo.save(p);}
 @PostMapping("/{id}/order") public ResponseEntity<String> order(@PathVariable Long id){ Optional<Product> op=repo.findById(id); if(op.isEmpty()) return ResponseEntity.notFound().build(); Product p=op.get(); String payload=String.format("{\"productId\":%d,\"name\":\"%s\",\"price\":%s}",p.getId(),p.getName(),p.getPrice()); kafka.send("product-orders",payload); return ResponseEntity.ok("order-sent"); }
}
