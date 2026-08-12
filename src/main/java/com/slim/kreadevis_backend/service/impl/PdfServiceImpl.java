package com.slim.kreadevis_backend.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.slim.kreadevis_backend.config.AppProperties;
import com.slim.kreadevis_backend.entity.Address;
import com.slim.kreadevis_backend.entity.Client;
import com.slim.kreadevis_backend.entity.Quote;
import com.slim.kreadevis_backend.entity.QuoteItem;
import com.slim.kreadevis_backend.repository.QuoteRepository;
import com.slim.kreadevis_backend.security.SecurityUtils;
import com.slim.kreadevis_backend.service.PdfService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PdfServiceImpl implements PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final QuoteRepository quoteRepository;
    private final AppProperties appProperties;
    private final ResourceLoader resourceLoader;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public byte[] generateQuotePdf(Long quoteId) {
        Quote quote = securityUtils.resolveOwned(
                        () -> quoteRepository.findByIdAndActiveTrue(quoteId),
                        ownerId -> quoteRepository.findByIdAndActiveTrueAndCreatedById(quoteId, ownerId))
                .orElseThrow(() -> new EntityNotFoundException("Quote not found: " + quoteId));
        return buildPdf(quote);
    }

    private byte[] buildPdf(Quote quote) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 70, 50);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            addHeader(document, "DEVIS");
            addQuoteInfo(document, quote);
            addClientInfo(document, quote.getClient());
            addItemsTable(document, quote);
            addTotal(document, quote);
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate PDF for quote " + quote.getId(), e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return out.toByteArray();
    }

    private void addHeader(Document document, String documentType) throws DocumentException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1, 2});

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPadding(4);
        loadLogo().ifPresent(logoBytes -> {
            try {
                Image logo = Image.getInstance(logoBytes);
                logo.scaleToFit(100, 60);
                logoCell.addElement(logo);
            } catch (BadElementException | IOException e) {
                log.warn("Could not embed logo in PDF: {}", e.getMessage());
            }
        });
        header.addCell(logoCell);

        AppProperties.Company company = appProperties.company();
        PdfPCell companyCell = new PdfPCell();
        companyCell.setBorder(Rectangle.NO_BORDER);
        companyCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        companyCell.setPadding(4);
        companyCell.addElement(new Paragraph(company.name(), new Font(Font.HELVETICA, 12, Font.BOLD)));
        if (company.address() != null && !company.address().isBlank()) {
            companyCell.addElement(new Paragraph(company.address(), new Font(Font.HELVETICA, 10)));
        }
        if (company.phone() != null && !company.phone().isBlank()) {
            companyCell.addElement(new Paragraph(company.phone(), new Font(Font.HELVETICA, 10)));
        }
        if (company.email() != null && !company.email().isBlank()) {
            companyCell.addElement(new Paragraph(company.email(), new Font(Font.HELVETICA, 10)));
        }
        if (company.siren() != null && !company.siren().isBlank()) {
            companyCell.addElement(new Paragraph("SIREN: " + company.siren(), new Font(Font.HELVETICA, 10)));
        }
        header.addCell(companyCell);
        document.add(header);

        Paragraph title = new Paragraph(documentType, new Font(Font.HELVETICA, 18, Font.BOLD));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(10);
        title.setSpacingAfter(10);
        document.add(title);
    }

    private void addQuoteInfo(Document document, Quote quote) throws DocumentException {
        Font normal = new Font(Font.HELVETICA, 10);
        if (quote.getReferenceCode() != null) {
            document.add(new Paragraph("Référence : " + quote.getReferenceCode(), normal));
        }
        document.add(new Paragraph("Date : " + quote.getDate().format(DATE_FORMAT), normal));
        document.add(new Paragraph(" "));
    }

    private void addClientInfo(Document document, Client client) throws DocumentException {
        document.add(new Paragraph("CLIENT", new Font(Font.HELVETICA, 11, Font.BOLD)));

        Font normal = new Font(Font.HELVETICA, 10);
        String fullName = (client.getFirstName() != null ? client.getFirstName() + " " : "") + client.getLastName();
        document.add(new Paragraph(fullName, normal));

        if (client.getCompany() != null && !client.getCompany().isBlank()) {
            document.add(new Paragraph(client.getCompany(), normal));
        }

        Address address = client.getAddress();
        if (address != null) {
            String street = (address.getStreetNumber() != null ? address.getStreetNumber() + " " : "")
                    + (address.getStreet() != null ? address.getStreet() : "");
            if (!street.isBlank()) {
                document.add(new Paragraph(street, normal));
            }
            String city = (address.getPostalCode() != null ? address.getPostalCode() + " " : "")
                    + (address.getCity() != null ? address.getCity() : "");
            if (!city.isBlank()) {
                document.add(new Paragraph(city, normal));
            }
        }

        if (client.getPhone() != null && !client.getPhone().isBlank()) {
            document.add(new Paragraph("Tél : " + client.getPhone(), normal));
        }
        if (client.getEmail() != null && !client.getEmail().isBlank()) {
            document.add(new Paragraph(client.getEmail(), normal));
        }
        document.add(new Paragraph(" "));
    }

    private void addItemsTable(Document document, Quote quote) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4, 1, 2, 2});
        table.setSpacingBefore(5);

        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
        Color headerBg = new Color(220, 220, 220);
        addHeaderCell(table, "Désignation", headerFont, headerBg);
        addHeaderCell(table, "Qté", headerFont, headerBg);
        addHeaderCell(table, "Prix unitaire HT", headerFont, headerBg);
        addHeaderCell(table, "Total HT", headerFont, headerBg);

        Font cellFont = new Font(Font.HELVETICA, 10);
        for (QuoteItem item : quote.getItems()) {
            if (!item.isActive()) continue;
            String label = item.getProduct() != null ? item.getProduct().getLabel() : "-";
            addDataCell(table, label, cellFont, Element.ALIGN_LEFT);
            addDataCell(table, String.valueOf(item.getQuantity()), cellFont, Element.ALIGN_CENTER);
            addDataCell(table, String.format("%.2f €", item.getUnitPrice()), cellFont, Element.ALIGN_RIGHT);
            addDataCell(table, String.format("%.2f €", item.getTotalPrice()), cellFont, Element.ALIGN_RIGHT);
        }

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addTotal(Document document, Quote quote) throws DocumentException {
        Font normalFont = new Font(Font.HELVETICA, 10);
        Font boldFont = new Font(Font.HELVETICA, 11, Font.BOLD);

        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(40);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        addTotalRow(totalTable, "Total HT", quote.getTotalPriceHt(), normalFont, true);
        addTotalRow(totalTable, "TVA",      quote.getTotalVat(),     normalFont, false);
        addTotalRow(totalTable, "TOTAL TTC", quote.getTotalPriceTtc(), boldFont, true);

        document.add(totalTable);
    }

    private void addTotalRow(PdfPTable table, String label, java.math.BigDecimal amount, Font font, boolean topBorder) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        labelCell.setPadding(5);
        labelCell.setBorderWidthTop(topBorder ? 1 : 0);
        labelCell.setBorderWidthBottom(0);
        labelCell.setBorderWidthLeft(0);
        labelCell.setBorderWidthRight(0);
        table.addCell(labelCell);

        String formatted = amount != null ? String.format("%.2f €", amount) : "—";
        PdfPCell valueCell = new PdfPCell(new Phrase(formatted, font));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(5);
        valueCell.setBorderWidthTop(topBorder ? 1 : 0);
        valueCell.setBorderWidthBottom(0);
        valueCell.setBorderWidthLeft(0);
        valueCell.setBorderWidthRight(0);
        table.addCell(valueCell);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private Optional<byte[]> loadLogo() {
        String logoPath = appProperties.document().logoPath();
        try {
            Resource resource = resourceLoader.getResource(logoPath);
            if (resource.exists()) {
                return Optional.of(resource.getContentAsByteArray());
            }
            log.debug("Logo resource not found at {}, skipping", logoPath);
        } catch (IOException e) {
            log.warn("Could not load logo from {}: {}", logoPath, e.getMessage());
        }
        return Optional.empty();
    }
}
