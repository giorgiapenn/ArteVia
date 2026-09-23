package com.artevia.mapper;

import com.artevia.dto.WalletDto;
import com.artevia.model.Wallet;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    WalletDto toDto(Wallet wallet);
}
