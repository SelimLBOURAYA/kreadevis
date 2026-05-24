package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.entity.Quote;
import com.slim.kreadevis_backend.entity.QuoteItem;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class QuoteTotals {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private QuoteTotals() {}

    static void recompute(Quote quote) {
        BigDecimal totalHt = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        for (QuoteItem item : quote.getItems()) {
            if (!item.isActive() || item.getTotalPrice() == null) continue;
            totalHt = totalHt.add(item.getTotalPrice());
            BigDecimal rate = item.getVatRate() != null ? item.getVatRate() : BigDecimal.ZERO;
            BigDecimal vatForLine = item.getTotalPrice()
                    .multiply(rate)
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
            totalVat = totalVat.add(vatForLine);
        }
        quote.setTotalPriceHt(totalHt);
        quote.setTotalVat(totalVat);
        quote.setTotalPriceTtc(totalHt.add(totalVat));
    }
}
