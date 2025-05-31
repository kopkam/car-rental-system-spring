package com.transport.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.transport.entity.Booking;
import com.transport.entity.Invoice;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    public byte[] generateInvoicePdf(Booking booking) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Header
            document.add(new Paragraph("CAR RENTAL INVOICE")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(20)
                    .setBold());

            document.add(new Paragraph("\n"));

            // Invoice details
            if (booking.getInvoice() != null) {
                Invoice invoice = booking.getInvoice();
                document.add(new Paragraph("Invoice Number: " + invoice.getInvoiceNumber()));
                document.add(new Paragraph("Issue Date: " + invoice.getIssueDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
            }

            document.add(new Paragraph("\n"));

            // Customer details
            document.add(new Paragraph("Customer Information:")
                    .setBold());
            document.add(new Paragraph("Name: " + booking.getCustomer().getFirstName() + " " + booking.getCustomer().getLastName()));
            document.add(new Paragraph("Email: " + booking.getCustomer().getEmail()));

            document.add(new Paragraph("\n"));

            // Booking details
            document.add(new Paragraph("Booking Details:")
                    .setBold());

            Table table = new Table(2);
            table.addCell("Car:");
            table.addCell(booking.getCar().getBrand() + " " + booking.getCar().getModel());
            table.addCell("License Plate:");
            table.addCell(booking.getCar().getLicensePlate());
            table.addCell("Start Date:");
            table.addCell(booking.getStartDate().toString());
            table.addCell("End Date:");
            table.addCell(booking.getEndDate().toString());
            table.addCell("Daily Rate:");
            table.addCell("$" + booking.getCar().getDailyRate().toString());
            table.addCell("Total Amount:");
            table.addCell("$" + booking.getTotalAmount().toString());

            document.add(table);

            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Thank you for choosing our car rental service!")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setItalic());

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }
}