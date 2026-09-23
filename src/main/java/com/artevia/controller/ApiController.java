package com.artevia.controller;

import com.artevia.dto.*;
import com.artevia.model.User;
import com.artevia.security.UserDetailsImpl;
import com.artevia.service.*;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiController {

    private final WalletService walletService;
    private final ShopService shopService;
    private final MembershipService membershipService;
    private final ArtworkOfDayService artworkOfDayService;
    private final RateLimiterRegistry rateLimiterRegistry;

    @GetMapping("/wallet/mywallet")
    public WalletDto myWallet(@AuthenticationPrincipal UserDetailsImpl principal) {
        return walletService.getWalletDto(currentUser(principal));
    }

    @PostMapping("/wallet/recharge")
    public WalletDto recharge(@AuthenticationPrincipal UserDetailsImpl principal, @Valid @RequestBody RechargeRequest request) {
        return walletService.rechargeDto(currentUser(principal), request.amount());
    }

    @GetMapping("/products")
    public List<ProductDto> products() {
        return shopService.listProducts();
    }

    @PostMapping("/shop/checkout")
    public ShopService.CheckoutResult checkout(@AuthenticationPrincipal UserDetailsImpl principal,
                                                @Valid @RequestBody CheckoutRequest request) {
        List<ShopService.CartItem> items = request.items().stream()
                .map(cartItem -> new ShopService.CartItem(cartItem.id(), cartItem.quantity()))
                .toList();
        return shopService.checkout(currentUser(principal), items);
    }

    @PostMapping("/shop/cart/preview")
    public ShopService.CartPreviewResult previewCart(@AuthenticationPrincipal UserDetailsImpl principal,
                                                       @Valid @RequestBody CheckoutRequest request) {
        List<ShopService.CartItem> items = request.items().stream()
                .map(cartItem -> new ShopService.CartItem(cartItem.id(), cartItem.quantity()))
                .toList();
        return shopService.previewCart(currentUser(principal), items);
    }

    @GetMapping("/plans")
    public List<ClubPlanDto> plans() {
        return membershipService.listPlans();
    }

    @PostMapping("/membership/buy")
    public void buyMembership(@AuthenticationPrincipal UserDetailsImpl principal, @Valid @RequestBody BuyMembershipRequest request) {
        membershipService.buyPlan(currentUser(principal), request.planId());
    }

    @GetMapping("/user/shop/history")
    public List<PurchasedProductDto> history(@AuthenticationPrincipal UserDetailsImpl principal) {
        return shopService.getPurchaseHistory(currentUser(principal));
    }

    @GetMapping("/artwork/featured")
    public ArtworkDto featuredArtwork(@AuthenticationPrincipal UserDetailsImpl principal) {
        var limiter = rateLimiterRegistry.rateLimiter("articApi-" + principal.getUsername(), "articApi");
        return RateLimiter.decorateSupplier(limiter, artworkOfDayService::getFeaturedArtwork).get();
    }

    @GetMapping("/artwork/image/{imageId}")
    public org.springframework.http.ResponseEntity<byte[]> artworkImage(@AuthenticationPrincipal UserDetailsImpl principal,
                                                                        @PathVariable String imageId) {
        var limiter = rateLimiterRegistry.rateLimiter("articImage-" + principal.getUsername(), "articApi");
        return RateLimiter.decorateSupplier(limiter, () -> artworkOfDayService.fetchImageBytes(imageId)).get();
    }

    private User currentUser(UserDetailsImpl principal) {
        return principal.getUser();
    }
}