package com.artevia.service;

import com.artevia.mapper.WalletMapper;
import com.artevia.model.User;
import com.artevia.model.Wallet;
import com.artevia.repository.WalletRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletMapper walletMapper;

    @InjectMocks
    private WalletService walletService;

    @Test
    void recharge_con_importo_negativo_lancia_eccezione() {
        User user = User.builder().username("gio").build();

        assertThatThrownBy(() -> walletService.recharge(user, new BigDecimal("-10")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Importo non valido");
    }

    @Test
    void recharge_con_importo_zero_lancia_eccezione() {
        User user = User.builder().username("gio").build();

        assertThatThrownBy(() -> walletService.recharge(user, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recharge_con_importo_valido_aggiorna_il_saldo() {
        User user = User.builder().username("gio").build();
        Wallet wallet = Wallet.builder().user(user).balance(new BigDecimal("10.00")).build();

        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Wallet result = walletService.recharge(user, new BigDecimal("5.00"));

        assertThat(result.getBalance()).isEqualByComparingTo("15.00");
        verify(walletRepository).save(wallet);
    }

    @Test
    void getWallet_senza_wallet_associato_lancia_eccezione() {
        User user = User.builder().username("gio").build();
        when(walletRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getWallet(user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Wallet non trovato");
    }
}
