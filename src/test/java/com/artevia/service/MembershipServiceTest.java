package com.artevia.service;

import com.artevia.mapper.ClubPlanMapper;
import com.artevia.model.*;
import com.artevia.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock private ClubPlanRepository clubPlanRepository;
    @Mock private ClubMembershipRepository clubMembershipRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private ClubPlanMapper clubPlanMapper;

    @InjectMocks
    private MembershipService membershipService;

    private final User user = User.builder().username("gio").build();

    @Test
    void buyPlan_con_piano_inesistente_lancia_eccezione() {
        when(clubPlanRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> membershipService.buyPlan(user, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Piano non trovato");
    }

    @Test
    void buyPlan_con_saldo_insufficiente_lancia_eccezione() {
        ClubPlan plan = ClubPlan.builder().id(1L).price(new BigDecimal("50")).build();
        Wallet wallet = Wallet.builder().user(user).balance(new BigDecimal("10")).build();

        when(clubPlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> membershipService.buyPlan(user, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Saldo insufficiente");

        verify(clubMembershipRepository, never()).save(any());
    }

    @Test
    void buyPlan_con_successo_fa_scadere_labbonamento_precedente_e_scala_il_saldo() {
        ClubPlan plan = ClubPlan.builder().id(1L).price(new BigDecimal("9.90")).durationDays(30).build();
        Wallet wallet = Wallet.builder().user(user).balance(new BigDecimal("50")).build();

        ClubMembership existing = ClubMembership.builder().active(true).build();

        when(clubPlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));
        when(clubMembershipRepository.findByUserAndActiveTrue(user)).thenReturn(Optional.of(existing));

        membershipService.buyPlan(user, 1L);

        assertThat(existing.isActive()).isFalse();
        assertThat(wallet.getBalance()).isEqualByComparingTo("40.10");
        verify(clubMembershipRepository).save(existing);
        verify(clubMembershipRepository).save(argThat(ClubMembership::isActive));
        verify(walletRepository).save(wallet);
    }
}
