package com.artevia.service;

import com.artevia.mapper.ProductMapper;
import com.artevia.mapper.PurchasedProductMapper;
import com.artevia.model.*;
import com.artevia.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private PurchasedProductRepository purchasedProductRepository;
    @Mock private ClubMembershipRepository clubMembershipRepository;
    @Mock private ProductMapper productMapper;
    @Mock private PurchasedProductMapper purchasedProductMapper;

    @InjectMocks
    private ShopService shopService;

    private final User user = User.builder().username("gio").build();

    @Test
    void checkout_con_prodotto_inesistente_lancia_eccezione() {
        Wallet wallet = Wallet.builder().user(user).balance(new BigDecimal("1000")).build();
        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        var items = List.of(new ShopService.CartItem(99L, 1));

        assertThatThrownBy(() -> shopService.checkout(user, items))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prodotto non trovato");
    }

    @Test
    void checkout_con_stock_insufficiente_lancia_eccezione() {
        Wallet wallet = Wallet.builder().user(user).balance(new BigDecimal("1000")).build();
        Product product = Product.builder().id(1L).name("Tela").price(new BigDecimal("50")).stockQuantity(1).build();

        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        var items = List.of(new ShopService.CartItem(1L, 5));

        assertThatThrownBy(() -> shopService.checkout(user, items))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock insufficiente");
    }

    @Test
    void checkout_con_saldo_insufficiente_lancia_eccezione_e_non_scala_lo_stock() {
        Wallet wallet = Wallet.builder().user(user).balance(new BigDecimal("10")).build();
        Product product = Product.builder().id(1L).name("Tela").price(new BigDecimal("50")).stockQuantity(10).build();

        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(clubMembershipRepository.findByUserAndActiveTrue(user)).thenReturn(Optional.empty());

        var items = List.of(new ShopService.CartItem(1L, 1));

        assertThatThrownBy(() -> shopService.checkout(user, items))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Saldo insufficiente");

        assertThat(product.getStockQuantity()).isEqualTo(10);
        verify(productRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void checkout_con_abbonamento_attivo_applica_lo_sconto_e_aggiorna_stock_e_saldo() {
        Wallet wallet = Wallet.builder().user(user).balance(new BigDecimal("100")).build();
        Product product = Product.builder().id(1L).name("Tela").price(new BigDecimal("50")).stockQuantity(10).build();

        ClubPlan plan = ClubPlan.builder().discountPercentage(10).build();
        ClubMembership membership = ClubMembership.builder().plan(plan).active(true).endDate(java.time.LocalDateTime.now().plusDays(30)).build();

        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(clubMembershipRepository.findByUserAndActiveTrue(user)).thenReturn(Optional.of(membership));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(purchasedProductRepository.save(any(PurchasedProduct.class))).thenAnswer(inv -> inv.getArgument(0));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        var items = List.of(new ShopService.CartItem(1L, 1));

        ShopService.CheckoutResult result = shopService.checkout(user, items);

        assertThat(result.totalSpent()).isEqualByComparingTo("45.00");
        assertThat(result.newBalance()).isEqualByComparingTo("55.00");
        assertThat(product.getStockQuantity()).isEqualTo(9);
        verify(purchasedProductRepository).save(any(PurchasedProduct.class));
    }

    @Test
    void checkout_con_abbonamento_scaduto_non_applica_lo_sconto() {
        Wallet wallet = Wallet.builder().user(user).balance(new BigDecimal("100")).build();
        Product product = Product.builder().id(1L).name("Tela").price(new BigDecimal("50")).stockQuantity(10).build();

        ClubPlan plan = ClubPlan.builder().discountPercentage(10).build();
        ClubMembership scaduta = ClubMembership.builder().plan(plan).active(true)
                .endDate(java.time.LocalDateTime.now().minusDays(1)).build();

        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(clubMembershipRepository.findByUserAndActiveTrue(user)).thenReturn(Optional.of(scaduta));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(purchasedProductRepository.save(any(PurchasedProduct.class))).thenAnswer(inv -> inv.getArgument(0));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        ShopService.CheckoutResult result = shopService.checkout(user, List.of(new ShopService.CartItem(1L, 1)));

        assertThat(result.totalSpent()).isEqualByComparingTo("50.00");
        assertThat(result.newBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    void checkout_con_quantita_in_overflow_viene_rifiutato_senza_toccare_il_database() {
        var items = List.of(new ShopService.CartItem(1L, Integer.MAX_VALUE),
                            new ShopService.CartItem(1L, Integer.MAX_VALUE));

        assertThatThrownBy(() -> shopService.checkout(user, items))
                .isInstanceOf(ArithmeticException.class);

        verifyNoInteractions(walletRepository, productRepository, purchasedProductRepository);
    }

    @Test
    void con_lo_sconto_il_totale_coincide_con_la_somma_dei_prezzi_unitari_registrati() {
        Wallet wallet = Wallet.builder().user(user).balance(new BigDecimal("200")).build();
        Product product = Product.builder().id(1L).name("Stampa").price(new BigDecimal("39.90")).stockQuantity(10).build();
        ClubPlan plan = ClubPlan.builder().discountPercentage(5).build();
        ClubMembership membership = ClubMembership.builder().plan(plan).active(true)
                .endDate(java.time.LocalDateTime.now().plusDays(30)).build();

        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(clubMembershipRepository.findByUserAndActiveTrue(user)).thenReturn(Optional.of(membership));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(purchasedProductRepository.save(any(PurchasedProduct.class))).thenAnswer(inv -> inv.getArgument(0));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        ShopService.CheckoutResult result = shopService.checkout(user, List.of(new ShopService.CartItem(1L, 3)));

        assertThat(result.totalSpent()).isEqualByComparingTo("113.73");
        assertThat(result.newBalance()).isEqualByComparingTo("86.27");
    }
}
