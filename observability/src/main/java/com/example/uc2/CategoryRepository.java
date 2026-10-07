package com.example.uc2;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link Category}, used to seed the catalog demo. */
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
