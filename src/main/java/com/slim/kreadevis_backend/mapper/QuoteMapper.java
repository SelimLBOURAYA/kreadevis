package com.slim.kreadevis_backend.mapper;

import com.slim.kreadevis_backend.dto.quote.QuoteItemResponse;
import com.slim.kreadevis_backend.dto.quote.QuoteResponse;
import com.slim.kreadevis_backend.entity.Quote;
import com.slim.kreadevis_backend.entity.QuoteItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {ClientMapper.class, ProductMapper.class})
public interface QuoteMapper {
    QuoteResponse toResponse(Quote quote);
    QuoteItemResponse toItemResponse(QuoteItem item);
}
