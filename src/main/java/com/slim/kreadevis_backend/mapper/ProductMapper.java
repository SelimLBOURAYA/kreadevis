package com.slim.kreadevis_backend.mapper;

import com.slim.kreadevis_backend.dto.product.ProductRequest;
import com.slim.kreadevis_backend.dto.product.ProductResponse;
import com.slim.kreadevis_backend.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(source = "supplier.id", target = "supplierId")
    ProductResponse toResponse(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    Product toEntity(ProductRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    void updateEntity(ProductRequest request, @MappingTarget Product product);
}
