package com.portfolio_hub.portfolio;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.portfolio_hub.portfolio.response.PublicPortfolioResponse;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PortfolioSharingService {

  private final PortfolioService portfolioService;

  @Value("${application.front-end-url}")
  private String frontEndUrl;

  public byte[] qrCode(String username) {
    portfolioService.getPublic(username);
    try {
      var matrix = new QRCodeWriter().encode(
        frontEndUrl.replaceAll("/$", "") + "/" + username,
        BarcodeFormat.QR_CODE,
        360,
        360
      );
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(matrix, "PNG", output);
      return output.toByteArray();
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to generate QR code", exception);
    }
  }

  public byte[] portfolioPdf(String username) {
    PublicPortfolioResponse profile = portfolioService.getPublic(username);
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      Document document = new Document();
      PdfWriter.getInstance(document, output);
      document.open();
      Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
      Font heading = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
      Font body = FontFactory.getFont(FontFactory.HELVETICA, 10);
      document.add(new Paragraph(profile.fullName(), title));
      add(document, profile.headline(), body);
      add(document, profile.introduction(), body);
      add(document, profile.note(), body);
      add(document, profile.availability(), body);
      if (profile.websiteUrl() != null) add(
        document,
        "Website: " + profile.websiteUrl(),
        body
      );
      if (!profile.skills().isEmpty()) {
        document.add(new Paragraph("Skills", heading));
        add(
          document,
          profile
            .skills()
            .stream()
            .map(skill -> skill.name() + " (" + skill.proficiency() + ")")
            .reduce((a, b) -> a + ", " + b)
            .orElse(""),
          body
        );
      }
      for (var type : com.portfolio_hub.profile.ProfileEntry.EntryType.values()) {
        var entries = profile
          .profileEntries()
          .stream()
          .filter(entry -> entry.type() == type)
          .toList();
        if (entries.isEmpty()) continue;
        document.add(new Paragraph(label(type.name()), heading));
        for (var entry : entries) {
          add(
            document,
            entry.title() +
              (entry.organization() == null
                  ? ""
                  : " — " + entry.organization()),
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)
          );
          String dates =
            date(entry.startDate()) +
            " – " +
            (entry.current() ? "Present" : date(entry.endDate()));
          add(
            document,
            dates + (entry.location() == null ? "" : " · " + entry.location()),
            body
          );
          add(document, entry.description(), body);
        }
      }
      if (!profile.works().isEmpty()) {
        document.add(new Paragraph("Selected Projects", heading));
        for (var work : profile.works()) {
          add(
            document,
            work.title() + " — " + work.category(),
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)
          );
          add(document, work.summary(), body);
          add(
            document,
            work.challenge() == null ? null : "Challenge: " + work.challenge(),
            body
          );
          add(
            document,
            work.process() == null ? null : "Process: " + work.process(),
            body
          );
          add(
            document,
            work.results() == null ? null : "Results: " + work.results(),
            body
          );
          if (!work.technologyStack().isEmpty()) add(
            document,
            "Technology: " + String.join(", ", work.technologyStack()),
            body
          );
          add(document, work.projectUrl(), body);
        }
      }
      document.close();
      return output.toByteArray();
    } catch (Exception exception) {
      throw new IllegalStateException(
        "Unable to generate portfolio PDF",
        exception
      );
    }
  }

  private void add(Document document, String value, Font font)
    throws Exception {
    if (value != null && !value.isBlank()) document.add(
      new Paragraph(value, font)
    );
  }

  private String date(java.time.LocalDate value) {
    return value == null
      ? ""
      : value.format(DateTimeFormatter.ofPattern("MMM yyyy"));
  }

  private String label(String value) {
    return (
      value.substring(0, 1) + value.substring(1).toLowerCase().replace('_', ' ')
    );
  }
}
