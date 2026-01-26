#!/usr/bin/env python3
"""
Fix product-service to automatically publish to Kafka when products are created.
"""

import os
import shutil
from datetime import datetime
from pathlib import Path

# Target file
CONTROLLER_FILE = Path("src/main/java/com/example/product/controller/ProductController.java")

def main():
    print("🔧 LeninKart Product-Service Auto-Fix")
    print("=" * 70)
    
    # Check if file exists
    if not CONTROLLER_FILE.exists():
        print(f"\n❌ ERROR: {CONTROLLER_FILE} not found!")
        print(f"   Current directory: {os.getcwd()}")
        print("\n💡 Make sure you're in: C:\\Users\\Lenovo\\Desktop\\dust\\leninkart-product-service")
        exit(1)
    
    # Create backup
    backup_dir = Path(f"_backup_{datetime.now().strftime('%Y%m%d_%H%M%S')}")
    backup_dir.mkdir(exist_ok=True)
    backup_file = backup_dir / CONTROLLER_FILE.name
    shutil.copy2(CONTROLLER_FILE, backup_file)
    print(f"✅ Backed up to: {backup_file}")
    
    # Write the fixed controller
    new_content = '''package com.example.product.controller;
import com.example.product.model.Product; 
import com.example.product.repo.ProductRepository; 
import org.springframework.http.ResponseEntity; 
import org.springframework.kafka.core.KafkaTemplate; 
import org.springframework.web.bind.annotation.*; 
import java.util.List; 
import java.util.Optional;

@RestController 
@RequestMapping("/api/products") 
public class ProductController {

    private final ProductRepository repo; 
    private final KafkaTemplate<String,String> kafka;

    public ProductController(ProductRepository repo, KafkaTemplate<String,String> kafka) {
        this.repo = repo;
        this.kafka = kafka;
    }

    @GetMapping 
    public List<Product> all() {
        return repo.findAll();
    }

    @PostMapping 
    public Product create(@RequestBody Product p) {
        // Save product to database
        Product saved = repo.save(p);
        
        // 🆕 AUTOMATICALLY publish to Kafka after saving
        try {
            String payload = String.format(
                "{\\"productId\\":%d,\\"name\\":\\"%s\\",\\"price\\":%s}",
                saved.getId(),
                saved.getName(),
                saved.getPrice()
            );
            kafka.send("product-orders", payload);
            System.out.println("✅ Published to Kafka: " + payload);
        } catch (Exception e) {
            System.err.println("❌ Failed to publish to Kafka: " + e.getMessage());
        }
        
        return saved;
    }

    @PostMapping("/{id}/order") 
    public ResponseEntity<String> order(@PathVariable Long id) {
        Optional<Product> op = repo.findById(id); 
        if (op.isEmpty()) return ResponseEntity.notFound().build(); 
        
        Product p = op.get(); 
        String payload = String.format(
            "{\\"productId\\":%d,\\"name\\":\\"%s\\",\\"price\\":%s}",
            p.getId(),
            p.getName(),
            p.getPrice()
        ); 
        kafka.send("product-orders", payload); 
        return ResponseEntity.ok("order-sent"); 
    }
}
'''
    
    CONTROLLER_FILE.write_text(new_content, encoding='utf-8')
    print(f"✅ Fixed: {CONTROLLER_FILE}")
    
    # Print next steps
    print("\n" + "="*70)
    print("🎉 FIX APPLIED SUCCESSFULLY!")
    print("="*70)
    
    print("\n📋 WHAT CHANGED:")
    print("   • Products now auto-publish to Kafka when created")
    print("   • Added error handling and logging")
    
    print("\n📝 NEXT STEPS:")
    print("\n1️⃣  Commit and push:")
    print("   git add .")
    print('   git commit -m "fix: auto-publish to Kafka on product creation"')
    print("   git push origin dev")
    
    print("\n2️⃣  Wait for CI/CD to rebuild (~3-5 min)")
    
    print("\n3️⃣  Test it:")
    print("   # Add a product")
    print('   curl http://localhost:8082/api/products -Method POST `')
    print('     -Headers @{"Content-Type"="application/json"} `')
    print('     -Body \'{"name":"Auto Test","price":99.99,"description":"Test"}\'')
    print()
    print("   # Check orders (should appear automatically!)")
    print("   curl http://localhost:8083/api/orders")
    
    print("\n" + "="*70)

if __name__ == "__main__":
    main()