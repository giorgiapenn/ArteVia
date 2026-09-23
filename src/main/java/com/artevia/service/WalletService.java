package com.artevia.service;

import com.artevia.dto.WalletDto;
import com.artevia.mapper.WalletMapper;
import com.artevia.model.User;
import com.artevia.model.Wallet;
import com.artevia.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletMapper walletMapper;

    public WalletDto getWalletDto(User user) {
        return walletMapper.toDto(getWallet(user));
    }

    @Transactional
    public WalletDto rechargeDto(User user, BigDecimal amount) {
        return walletMapper.toDto(recharge(user, amount));
    }

    public Wallet getWallet(User user) {
        return walletRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Wallet non trovato"));
    }

    @Transactional
    public Wallet recharge(User user, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Importo non valido");

        var wallet = getWallet(user);
        wallet.setBalance(wallet.getBalance().add(amount));
        return walletRepository.save(wallet);
    }
}
