package com.artevia.mapper;

import com.artevia.dto.PurchasedProductDto;
import com.artevia.model.PurchasedProduct;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PurchasedProductMapper {

    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "total", expression = "java(purchasedProduct.getPriceAtPurchase()"
            + ".multiply(java.math.BigDecimal.valueOf(purchasedProduct.getQuantity())))")
    PurchasedProductDto toDto(PurchasedProduct purchasedProduct);
}
