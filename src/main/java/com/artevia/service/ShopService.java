package com.artevia.service;

import com.artevia.dto.ProductDto;
import com.artevia.dto.PurchasedProductDto;
import com.artevia.mapper.ProductMapper;
import com.artevia.mapper.PurchasedProductMapper;
import com.artevia.model.*;
import com.artevia.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ProductRepository productRepository;
    private final WalletRepository walletRepository;
    private final PurchasedProductRepository purchasedProductRepository;
    private final ClubMembershipRepository clubMembershipRepository;
    private final ProductMapper productMapper;
    private final PurchasedProductMapper purchasedProductMapper;

    public record CartItem(Long productId, Integer quantity) {}
    public record CheckoutResult(String message, BigDecimal totalSpent, BigDecimal newBalance) {}
    public record CartPreviewResult(BigDecimal originalTotal, BigDecimal discountedTotal, int discountPercentage) {}

    public List<ProductDto> listProducts() {
        return productRepository.findAll().stream()
                .map(productMapper::toDto)
                .toList();
    }

    public List<PurchasedProductDto> getPurchaseHistory(User user) {
        return purchasedProductRepository.findByUserOrderByPurchasedAtDesc(user).stream()
                .map(purchasedProductMapper::toDto)
                .toList();
    }

    @Transactional
    public CheckoutResult checkout(User user, List<CartItem> items) {
        items = mergeByProduct(items);

        var wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Wallet non trovato"));

        var productsById = validateAndCollectProducts(items);
        var discountPercentage = activeDiscountPercentage(user);
        var total = computeDiscountedTotal(items, productsById, discountPercentage);

        if (wallet.getBalance().compareTo(total) < 0) {
            throw new IllegalArgumentException("Saldo insufficiente");
        }

        applyPurchase(user, items, productsById, wallet, total, discountPercentage);

        return new CheckoutResult("Acquisto completato con successo!", total, wallet.getBalance());
    }

    @Transactional(readOnly = true)
    public CartPreviewResult previewCart(User user, List<CartItem> items) {
        items = mergeByProduct(items);
        var productsById = validateAndCollectProducts(items);

        var originalTotal = BigDecimal.ZERO;
        for (var item : items) {
            var product = productsById.get(item.productId());
            originalTotal = originalTotal.add(product.getPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }
        originalTotal = originalTotal.setScale(2, RoundingMode.HALF_UP);

        var discountPercentage = activeDiscountPercentage(user);
        var discountedTotal = computeDiscountedTotal(items, productsById, discountPercentage);

        return new CartPreviewResult(originalTotal, discountedTotal, discountPercentage);
    }

    private List<CartItem> mergeByProduct(List<CartItem> items) {
        var quantities = new LinkedHashMap<Long, Integer>();
        for (var item : items) {
            quantities.merge(item.productId(), item.quantity(), Math::addExact);
        }
        return quantities.entrySet().stream()
                .map(e -> new CartItem(e.getKey(), e.getValue()))
                .toList();
    }

    private int activeDiscountPercentage(User user) {
        return clubMembershipRepository.findByUserAndActiveTrue(user)
                .filter(m -> m.getEndDate() != null && m.getEndDate().isAfter(java.time.LocalDateTime.now()))
                .map(membership -> membership.getPlan().getDiscountPercentage())
                .orElse(0);
    }

    private Map<Long, Product> validateAndCollectProducts(List<CartItem> items) {
        var productsById = new HashMap<Long, Product>();
        for (var item : items) {
            var product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato: " + item.productId()));

            if (product.getStockQuantity() < item.quantity()) {
                throw new IllegalArgumentException("Stock insufficiente per: " + product.getName());
            }
            productsById.put(product.getId(), product);
        }
        return productsById;
    }

    private BigDecimal discountedUnitPrice(Product product, int discountPercentage) {
        var discountFactor = BigDecimal.valueOf(100 - discountPercentage).divide(BigDecimal.valueOf(100));
        return product.getPrice().multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal computeDiscountedTotal(List<CartItem> items, Map<Long, Product> productsById,
                                               int discountPercentage) {
        var total = BigDecimal.ZERO;
        for (var item : items) {
            var unitPrice = discountedUnitPrice(productsById.get(item.productId()), discountPercentage);
            total = total.add(unitPrice.multiply(BigDecimal.valueOf(item.quantity())));
        }
        return total;
    }

    private void applyPurchase(User user, List<CartItem> items, Map<Long, Product> productsById,
                                Wallet wallet, BigDecimal total, int discountPercentage) {

        for (var item : items) {
            var product = productsById.get(item.productId());
            product.setStockQuantity(product.getStockQuantity() - item.quantity());
            productRepository.save(product);

            var paidUnitPrice = discountedUnitPrice(product, discountPercentage);

            var purchase = PurchasedProduct.builder()
                    .user(user)
                    .product(product)
                    .quantity(item.quantity())
                    .priceAtPurchase(paidUnitPrice)
                    .build();
            purchasedProductRepository.save(purchase);
        }

        wallet.setBalance(wallet.getBalance().subtract(total));
        walletRepository.save(wallet);
    }
}