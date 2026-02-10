package com.example.product.model;
import jakarta.persistence.*;
@Entity @Table(name="products")
public class Product {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private String name; private String description; private Double price; private String createdBy;
 public Product(){} public Product(String name,String description,Double price,String createdBy){this.name=name;this.description=description;this.price=price;this.createdBy=createdBy;}
 public Long getId(){return id;} public String getName(){return name;} public String getDescription(){return description;} public Double getPrice(){return price;} public String getCreatedBy(){return createdBy;}
 public void setName(String n){this.name=n;} public void setDescription(String d){this.description=d;} public void setPrice(Double p){this.price=p;} public void setCreatedBy(String u){this.createdBy=u;}
}
