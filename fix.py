#!/usr/bin/env python3
"""
Fix product-service to automatically publish to Kafka when products are created.
This eliminates the need for the separate /order endpoint.
"""

import os
import shutil
from datetime import datetime
from pathlib import Path

# Target file
CONTROLLER_FILE = Path("src/main/java/com/example/product/controller/ProductController.java")
BACKUP_DIR = Path(f"_backup_{datetime.now().strftime('%Y%m%d_%H%M%S')}")

def create_backup():
    """Backup the original file"""
    if CONTROLLER_FILE.exists():
        BACKUP_DIR.mkdir(exist_ok=True)
        backup_file = BACKUP_DIR / CONTROLLER_FILE.name
        backup_file.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(CONTROLLER_FILE, backup_file)
        print(f"✅ Backed up to: {backup_file}")
    else:
        print(f"❌ File not found: {CONTROLLER_FILE}")
        print(f"   Current directory: {os.getcwd()}")
        print(f"   Make sure you're in the leninkart-product-service directory!")
        exit(1)

def fix_controller():
    """Fix the ProductController to auto-publish to Kafka on product creation"""
    
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

def print_next_steps():
    """Print what to do next"""
    print("\n" + "="*70)
    print("🎉 FIX APPLIED SUCCESSFULLY!")
    print("="*70)
    
    print("\n📋 WHAT CHANGED:")
    print("   • The @PostMapping create() method now automatically publishes to Kafka")
    print("   • Every new product will trigger an order message")
    print("   • Added error handling and logging")
    
    print("\n📝 NEXT STEPS:")
    print("\n1️⃣  Commit and push changes:")
    print("   git add .")
    print('   git commit -m "fix: auto-publish to Kafka on product creation"')
    print("   git push origin dev")
    
    print("\n2️⃣  Wait for CI/CD to rebuild and deploy (~3-5 minutes)")
    print("   • Watch GitHub Actions: https://github.com/Leninfitfreak/leninkart-product-service/actions")
    print("   • Or check ArgoCD dashboard")
    
    print("\n3️⃣  Verify the fix:")
    print("   # Port-forward if needed")
    print("   kubectl port-forward -n dev svc/leninkart-product-service 8082:8081")
    print()
    print("   # In another terminal, watch order-service logs")
    print("   kubectl logs -n dev -l app.kubernetes.io/name=order-service -f")
    print()
    print("   # Create a product")
    print('   curl http://localhost:8082/api/products -Method POST `')
    print('     -Headers @{"Content-Type"="application/json"} `')
    print('     -Body \'{"name":"Auto Test","price":123.45,"description":"Testing auto-publish"}\'')
    print()
    print("   # Check orders (should appear automatically!)")
    print("   curl http://localhost:8083/api/orders")
    
    print("\n4️⃣  Test via UI:")
    print("   • Add a product through the frontend")
    print("   • It should automatically appear in Orders!")
    
    print("\n" + "="*70)
    print(f"📂 Backup saved to: {BACKUP_DIR}")
    print("="*70)

def main():
    print("🔧 LeninKart Product-Service Auto-Fix")
    print("=" * 70)
    
    # Check if we're in the right directory
    if not CONTROLLER_FILE.exists():
        print(f"\n❌ ERROR: {CONTROLLER_FILE} not found!")
        print(f"   Current directory: {os.getcwd()}")
        print("\n💡 Make sure you run this from the leninkart-product-service directory:")
        print("   cd C:\\Users\\Lenovo\\Desktop\\dust\\leninkart-product-service")
        print("   python fix.py")
        exit(1)
    
    # Create backup
    print("\n📦 Creating backup...")
    create_backup()
    
    # Apply fix
    print("\n🔧 Applying fix...")
    fix_controller()
    
    # Print next steps
    print_next_steps()

if __name__ == "__main__":
    main()