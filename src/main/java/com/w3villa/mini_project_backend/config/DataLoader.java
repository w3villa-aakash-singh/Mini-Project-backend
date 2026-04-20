//package com.w3villa.mini_project_backend.config;
//
//import com.w3villa.mini_project_backend.entites.Category;
//import com.w3villa.mini_project_backend.entites.Product;
//import com.w3villa.mini_project_backend.repositories.CategoryRepository;
//import com.w3villa.mini_project_backend.repositories.ProductRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.*;
//
//@Configuration
//@RequiredArgsConstructor
//public class DataLoader implements CommandLineRunner {
//
//    private final CategoryRepository categoryRepo;
//    private final ProductRepository productRepo;
//
//    @Override
//    public void run(String... args) {
//
//        // ⚠️ Prevent duplicate inserts
//        if (productRepo.count() > 0) {
//            System.out.println("⚠️ Data already exists, skipping...");
//            return;
//        }
//
//        // ✅ Create categories
//        Category electronics = new Category();
//        electronics.setName("Electronics");
//
//        Category fashion = new Category();
//        fashion.setName("Fashion");
//
//        Category books = new Category();
//        books.setName("Books");
//
//        Category home = new Category();
//        home.setName("Home");
//
//        Category sports = new Category();
//        sports.setName("Sports");
//
//        categoryRepo.saveAll(List.of(electronics, fashion, books, home, sports));
//
//        List<Category> categories = categoryRepo.findAll();
//
//        // ✅ Real working images (no 403 issue
//
//        Random random = new Random();
//
//        // ✅ Generate products
//        List<Product> products = new ArrayList<>();
//
//        for (int i = 1; i <= 120; i++) {
//
//            Category category = categories.get(random.nextInt(categories.size()));
//
//            Product p = new Product();
//            p.setName(generateProductName(category.getName(), i));
//            p.setDescription("Premium " + category.getName() + " product");
//            p.setBasePrice(500.0 + random.nextInt(5000));
//
//            // ✅ FIXED IMAGE
//            p.setImageUrl("https://picsum.photos/400/300?random=" + i);
//
//            p.setCategory(category);
//
//            products.add(p);
//        }
//
//        productRepo.saveAll(products);
//
//        System.out.println("🔥 Seeded 5 categories + 120 products");
//    }
//
//    // ✅ Better product names
//    private String generateProductName(String category, int i) {
//        return switch (category) {
//            case "Electronics" -> "Smart Device " + i;
//            case "Fashion" -> "Stylish Wear " + i;
//            case "Books" -> "Book Edition " + i;
//            case "Home" -> "Home Utility " + i;
//            case "Sports" -> "Sports Gear " + i;
//            default -> "Product " + i;
//        };
//    }
//}