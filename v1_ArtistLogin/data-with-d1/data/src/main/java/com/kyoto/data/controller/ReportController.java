package com.kyoto.data.controller;

import com.kyoto.data.service.D1Service;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
public class ReportController {

    @Autowired
    private D1Service d1Service;

    // Helper method to execute queries depending on the chosen filter type
    private List<Map<String, Object>> fetchReportData(String filterType) {
        String sql;
        if ("topArtists".equalsIgnoreCase(filterType)) {
            // Ranks artists by who uploaded the most songs, listing total tracks and cumulative streams
            sql = "SELECT a.ArtistID, a.Name, a.Gender, COUNT(s.SongID) as TotalSongs, COALESCE(SUM(s.PlayCount), 0) as CumulativeStreams " +
                    "FROM ARTIST a " +
                    "LEFT JOIN SONG s ON a.ArtistID = s.ArtistID " +
                    "GROUP BY a.ArtistID, a.Name, a.Gender " +
                    "ORDER BY TotalSongs DESC";
            return d1Service.getResults(d1Service.executeQuery(sql));
        } else if ("premiumListeners".equalsIgnoreCase(filterType)) {
            // Filters listeners to show premium accounts alongside their chosen plan descriptions
            sql = "SELECT l.ListenerID, l.Name, l.Gender, p.PlanName, p.Price " +
                    "FROM LISTENER l " +
                    "LEFT JOIN PLAN p ON l.PlanID = p.PlanID " +
                    "WHERE l.isPremium = 1";
            return d1Service.getResults(d1Service.executeQuery(sql));
        } else {
            // Default: General Platform Song Catalog Insights
            sql = "SELECT s.SongID, s.Title, a.Name as ArtistName, s.PlayCount, s.DownloadCount " +
                    "FROM SONG s " +
                    "LEFT JOIN ARTIST a ON s.ArtistID = a.ArtistID " +
                    "ORDER BY s.PlayCount DESC";
            return d1Service.getResults(d1Service.executeQuery(sql));
        }
    }

    @GetMapping("/admin/reports")
    public String reportPage(@RequestParam String adminId,
                             @RequestParam(defaultValue = "topArtists") String filterType,
                             Model model) {
        var admin = d1Service.getResults(d1Service.executeQueryWithParams("SELECT Name FROM ADMIN WHERE AdminID = ?", List.of(adminId)));
        List<Map<String, Object>> reportData = fetchReportData(filterType);

        model.addAttribute("adminName", admin.get(0).get("Name"));
        model.addAttribute("adminId", adminId);
        model.addAttribute("filterType", filterType);
        model.addAttribute("reportData", reportData);

        return "adminReports"; // Renders adminReports.html
    }

    @GetMapping("/admin/reports/download")
    public ResponseEntity<byte[]> downloadReport(@RequestParam String filterType, @RequestParam String format) throws IOException {
        List<Map<String, Object>> data = fetchReportData(filterType);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String filename = "Kyoto_" + filterType + "_Report";

        if ("pdf".equalsIgnoreCase(format)) {
            // Explicitly use Lowagie's Document class to avoid conflicts with Apache POI
            com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4);
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Kyoto Music Platform - Management Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            document.add(new Paragraph("Filter Applied: " + filterType));
            document.add(new Paragraph(" ")); // Spacer

            if (!data.isEmpty()) {
                var firstRow = data.get(0);
                PdfPTable table = new PdfPTable(firstRow.size());
                table.setWidthPercentage(100);

                // Headers
                for (String key : firstRow.keySet()) {
                    table.addCell(key);
                }
                // Data rows
                for (Map<String, Object> row : data) {
                    for (Object val : row.values()) {
                        table.addCell(val != null ? val.toString() : "N/A");
                    }
                }
                document.add(table);
            }
            document.close();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(out.toByteArray());

        } else {
            // DOCX generation
            XWPFDocument document = new XWPFDocument();
            XWPFParagraph title = document.createParagraph();
            XWPFRun run = title.createRun();
            run.setText("Kyoto Music Platform - Management Report");
            run.setBold(true);
            run.setFontSize(16);

            if (!data.isEmpty()) {
                var firstRow = data.get(0);
                XWPFTable table = document.createTable(data.size() + 1, firstRow.size());

                // Set Header values
                int colIndex = 0;
                XWPFTableRow headerRow = table.getRow(0);
                for (String key : firstRow.keySet()) {
                    headerRow.getCell(colIndex++).setText(key);
                }

                // Set Data values
                int rowIndex = 1;
                for (Map<String, Object> row : data) {
                    XWPFTableRow tableRow = table.getRow(rowIndex++);
                    int cIndex = 0;
                    for (Object val : row.values()) {
                        tableRow.getCell(cIndex++).setText(val != null ? val.toString() : "N/A");
                    }
                }
            }
            document.write(out);
            document.close();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename + ".docx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .body(out.toByteArray());
        }
    }
}