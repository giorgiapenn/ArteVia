package com.artevia.service;

import com.artevia.dto.ClubPlanDto;
import com.artevia.mapper.ClubPlanMapper;
import com.artevia.model.*;
import com.artevia.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final ClubPlanRepository clubPlanRepository;
    private final ClubMembershipRepository clubMembershipRepository;
    private final WalletRepository walletRepository;
    private final ClubPlanMapper clubPlanMapper;

    public List<ClubPlanDto> listPlans() {
        return clubPlanRepository.findAll().stream()
                .map(clubPlanMapper::toDto)
                .toList();
    }

    @Transactional
    public void buyPlan(User user, Long planId) {
        var plan = clubPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Piano non trovato"));

        var wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Wallet non trovato"));

        if (wallet.getBalance().compareTo(plan.getPrice()) < 0) {
            throw new IllegalArgumentException("Saldo insufficiente");
        }

        expireActiveMembership(user);
        createMembership(user, plan);

        wallet.setBalance(wallet.getBalance().subtract(plan.getPrice()));
        walletRepository.save(wallet);
    }

    private void expireActiveMembership(User user) {
        clubMembershipRepository.findByUserAndActiveTrue(user).ifPresent(existingMembership -> {
            existingMembership.setActive(false);
            clubMembershipRepository.save(existingMembership);
        });
    }

    private void createMembership(User user, ClubPlan plan) {
        var membership = ClubMembership.builder()
                .user(user)
                .plan(plan)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(plan.getDurationDays()))
                .active(true)
                .build();
        clubMembershipRepository.save(membership);
    }
}
