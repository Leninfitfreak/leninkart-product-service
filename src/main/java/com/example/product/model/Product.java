package com.example.product.model;
import jakarta.persistence.*;
@Entity @Table(name="products")
public class Product {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private String name; private String description; private Double price;
 public Product(){} public Product(String name,String description,Double price){this.name=name;this.description=description;this.price=price;}
 public Long getId(){return id;} public String getName(){return name;} public String getDescription(){return description;} public Double getPrice(){return price;}
 public void setName(String n){this.name=n;} public void setDescription(String d){this.description=d;} public void setPrice(Double p){this.price=p;}
}
