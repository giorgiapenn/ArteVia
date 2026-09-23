package com.artevia.repository;

import com.artevia.model.PurchasedProduct;
import com.artevia.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PurchasedProductRepository extends JpaRepository<PurchasedProduct, Long> {
    List<PurchasedProduct> findByUserOrderByPurchasedAtDesc(User user);
}