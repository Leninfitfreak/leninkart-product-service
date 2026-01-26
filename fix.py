#!/usr/bin/env python3
"""
Universal fix for product-service to auto-publish to Kafka on product creation.
Works for BOTH local Docker Compose AND Kubernetes deployments.
"""

import os
import shutil
from datetime import datetime
from pathlib import Path

def find_product_controller():
    """Find the ProductController.java file in multiple possible locations"""
    possible_paths = [
        # Kubernetes repo structure
        Path("src/main/java/com/example/product/controller/ProductController.java"),
        # Local monorepo structure
        Path("leninkart-product-service/src/main/java/com/example/product/controller/ProductController.java"),
        # If running from specific service directory
        Path("../leninkart-product-service/src/main/java/com/example/product/controller/ProductController.java"),
    ]
    
    for path in possible_paths:
        if path.exists():
            return path
    
    return None

def create_backup(controller_file):
    """Backup the original file"""
    backup_dir = Path(f"_backup_{datetime.now().strftime('%Y%m%d_%H%M%S')}")
    backup_dir.mkdir(exist_ok=True)
    backup_file = backup_dir / controller_file.name
    shutil.copy2(controller_file, backup_file)
    print(f"✅ Backed up to: {backup_file}")
    return backup_dir

def fix_controller(controller_file):
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
            e.printStackTrace();
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
    
    controller_file.write_text(new_content, encoding='utf-8')
    print(f"✅ Fixed: {controller_file}")

def print_next_steps_local():
    """Instructions for local Docker Compose"""
    print("\n" + "="*70)
    print("📦 LOCAL DOCKER COMPOSE - NEXT STEPS")
    print("="*70)
    
    print("\n1️⃣  Rebuild and restart the service:")
    print("   cd C:\\Projects\\leninkart")
    print("   docker compose stop leninkart-product-service")
    print("   docker compose build leninkart-product-service")
    print("   docker compose up -d leninkart-product-service")
    
    print("\n2️⃣  Watch the logs:")
    print("   docker compose logs -f leninkart-product-service")
    
    print("\n3️⃣  Test it:")
    print("   # Add a product via UI at http://localhost:3000")
    print("   # Or via curl:")
    print('   curl http://localhost:8081/api/products -Method POST `')
    print('     -Headers @{"Content-Type"="application/json"} `')
    print('     -Body \'{"name":"Auto Test","price":99.99,"description":"Testing"}\'')
    
    print("\n4️⃣  Check orders:")
    print("   curl http://localhost:8082/api/orders")
    print("   # Should see the order automatically!")

def print_next_steps_k8s():
    """Instructions for Kubernetes deployment"""
    print("\n" + "="*70)
    print("☸️  KUBERNETES - NEXT STEPS")
    print("="*70)
    
    print("\n1️⃣  Commit and push changes:")
    print("   git add .")
    print('   git commit -m "fix: auto-publish to Kafka on product creation"')
    print("   git push origin dev")
    
    print("\n2️⃣  Wait for CI/CD to rebuild (~3-5 minutes)")
    print("   • GitHub Actions will build new image")
    print("   • ArgoCD will deploy automatically")
    
    print("\n3️⃣  Verify deployment:")
    print("   kubectl get pods -n dev")
    print("   kubectl logs -n dev -l app=product-service -f")
    
    print("\n4️⃣  Test it:")
    print("   kubectl port-forward -n dev svc/leninkart-product-service 8082:8081")
    print('   curl http://localhost:8082/api/products -Method POST `')
    print('     -Headers @{"Content-Type"="application/json"} `')
    print('     -Body \'{"name":"K8s Test","price":99.99,"description":"Testing"}\'')
    
    print("\n5️⃣  Check orders:")
    print("   kubectl port-forward -n dev svc/leninkart-order-service 8083:8080")
    print("   curl http://localhost:8083/api/orders")

def main():
    print("🔧 LeninKart Product-Service Universal Auto-Fix")
    print("=" * 70)
    
    # Find the controller file
    print("\n🔍 Looking for ProductController.java...")
    controller_file = find_product_controller()
    
    if not controller_file:
        print("\n❌ ERROR: ProductController.java not found!")
        print(f"   Current directory: {os.getcwd()}")
        print("\n💡 Make sure you're in one of these directories:")
        print("   • C:\\Users\\Lenovo\\Desktop\\dust\\leninkart-product-service  (K8s)")
        print("   • C:\\Projects\\leninkart  (Local Docker)")
        exit(1)
    
    print(f"✅ Found: {controller_file}")
    
    # Detect environment
    is_local = "leninkart-product-service" in str(controller_file) and "leninkart" in str(controller_file.parent.parent.parent.parent.parent)
    
    if is_local:
        print("🐳 Detected: LOCAL DOCKER COMPOSE environment")
    else:
        print("☸️  Detected: KUBERNETES deployment environment")
    
    # Create backup
    print("\n📦 Creating backup...")
    backup_dir = create_backup(controller_file)
    
    # Apply fix
    print("\n🔧 Applying fix...")
    fix_controller(controller_file)
    
    # Print appropriate next steps
    print("\n" + "="*70)
    print("🎉 FIX APPLIED SUCCESSFULLY!")
    print("="*70)
    
    print("\n📋 WHAT CHANGED:")
    print("   • @PostMapping create() now auto-publishes to Kafka")
    print("   • Every new product triggers an order message")
    print("   • Added error handling and logging")
    
    if is_local:
        print_next_steps_local()
    else:
        print_next_steps_k8s()
    
    print("\n" + "="*70)
    print(f"📂 Backup saved to: {backup_dir}")
    print("="*70)

if __name__ == "__main__":
    main()