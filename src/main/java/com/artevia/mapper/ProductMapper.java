package com.artevia.mapper;

import com.artevia.dto.ProductCreateRequest;
import com.artevia.dto.ProductDto;
import com.artevia.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductDto toDto(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    Product toEntity(ProductCreateRequest request);
}
