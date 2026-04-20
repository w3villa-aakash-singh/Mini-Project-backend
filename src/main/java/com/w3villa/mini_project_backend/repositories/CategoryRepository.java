package com.w3villa.mini_project_backend.repositories;

import com.w3villa.mini_project_backend.entites.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Basic CRUD (Save, Find, Delete) is automatically handled
    Category findByName(String name);
}