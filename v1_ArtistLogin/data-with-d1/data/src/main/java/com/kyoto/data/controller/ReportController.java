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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class ReportController {

    @Autowired
    private D1Service d1Service;

    private List<Map<String, Object>> fetchReportData(String filterType, String customTable, String customMetric, String customOrder) throws Exception {
        String sql;

        switch (filterType != null ? filterType : "topArtists") {
            case "topArtists":
                sql = "SELECT a.ArtistID, a.Name, a.Gender, COUNT(s.SongID) as TotalSongs, COALESCE(SUM(s.PlayCount), 0) as CumulativeStreams " +
                        "FROM ARTIST a LEFT JOIN SONG s ON a.ArtistID = s.ArtistID " +
                        "GROUP BY a.ArtistID, a.Name, a.Gender ORDER BY TotalSongs DESC";
                break;
            case "leastSongs":
                sql = "SELECT a.ArtistID, a.Name, a.Gender, COUNT(s.SongID) as TotalSongs " +
                        "FROM ARTIST a LEFT JOIN SONG s ON a.ArtistID = s.ArtistID " +
                        "GROUP BY a.ArtistID, a.Name, a.Gender ORDER BY TotalSongs ASC";
                break;
            case "maleArtists":
                sql = "SELECT ArtistID, Name, DateAccountCreated, Email FROM ARTIST WHERE Gender = 'M' ORDER BY Name ASC";
                break;
            case "femaleArtists":
                sql = "SELECT ArtistID, Name, DateAccountCreated, Email FROM ARTIST WHERE Gender = 'F' ORDER BY Name ASC";
                break;
            case "mostStreams":
                sql = "SELECT s.SongID, s.Title, a.Name as Artist, g.GenreName, s.PlayCount " +
                        "FROM SONG s LEFT JOIN ARTIST a ON s.ArtistID = a.ArtistID LEFT JOIN GENRE g ON s.GenreID = g.GenreID " +
                        "ORDER BY s.PlayCount DESC";
                break;
            case "leastStreams":
                sql = "SELECT s.SongID, s.Title, a.Name as Artist, s.PlayCount FROM SONG s " +
                        "LEFT JOIN ARTIST a ON s.ArtistID = a.ArtistID ORDER BY s.PlayCount ASC";
                break;
            case "mostPopularEvent":
                sql = "SELECT e.EventID, e.Title, a.Name as Headliner, e.Venue, e.TicketsSold, e.TotalTickets " +
                        "FROM EVENT e LEFT JOIN ARTIST a ON e.ArtistID = a.ArtistID ORDER BY e.TicketsSold DESC";
                break;
            case "mostEventRevenue":
                sql = "SELECT Title, Venue, EventDate, TicketPrice, TicketsSold, (TicketsSold * TicketPrice) as GrossRevenue FROM EVENT ORDER BY GrossRevenue DESC";
                break;
            case "highestTicketPrice":
                sql = "SELECT Title, Venue, EventDate, TicketPrice, TotalTickets FROM EVENT ORDER BY TicketPrice DESC";
                break;
            case "topSellingMerch":
                sql = "SELECT p.ProductID, p.Name, a.Name as Artist, p.Price, SUM(o.Quantity) as UnitsSold, SUM(o.TotalAmount) as TotalRevenue " +
                        "FROM PRODUCT_ORDER o LEFT JOIN PRODUCT p ON o.ProductID = p.ProductID LEFT JOIN ARTIST a ON p.ArtistID = a.ArtistID " +
                        "GROUP BY p.ProductID, p.Name, a.Name, p.Price ORDER BY UnitsSold DESC";
                break;
            case "lowStockAlert":
                sql = "SELECT p.ProductID, p.Name, a.Name as Artist, p.Category, p.Stock FROM PRODUCT p " +
                        "LEFT JOIN ARTIST a ON p.ArtistID = a.ArtistID WHERE p.Stock < 10 ORDER BY p.Stock ASC";
                break;
            case "popularGenres":
                sql = "SELECT g.GenreName, COUNT(s.SongID) as TotalTracks, COALESCE(SUM(s.PlayCount), 0) as AggregatePlays " +
                        "FROM GENRE g LEFT JOIN SONG s ON g.GenreID = s.GenreID GROUP BY g.GenreID, g.GenreName ORDER BY AggregatePlays DESC";
                break;

            case "customBuilder":
                // Completely safe mapping controls to handle the expressive query matrix configurations
                if ("ARTIST".equalsIgnoreCase(customTable)) {
                    sql = "SELECT ArtistID, Name, Gender, DateAccountCreated, Email FROM ARTIST ORDER BY " +
                            ("DateAccountCreated".equalsIgnoreCase(customMetric) ? "DateAccountCreated " : "Name ") + customOrder;
                } else if ("SONG".equalsIgnoreCase(customTable)) {
                    String sortCol = "DownloadCount".equalsIgnoreCase(customMetric) ? "DownloadCount" :
                            "Duration".equalsIgnoreCase(customMetric) ? "Duration" : "PlayCount";
                    sql = "SELECT SongID, Title, Duration, PlayCount, DownloadCount FROM SONG ORDER BY " + sortCol + " " + customOrder;
                } else if ("EVENT".equalsIgnoreCase(customTable)) {
                    String sortCol = "TicketPrice".equalsIgnoreCase(customMetric) ? "TicketPrice" :
                            "TotalTickets".equalsIgnoreCase(customMetric) ? "TotalTickets" : "TicketsSold";
                    sql = "SELECT EventID, Title, Venue, EventDate, TicketPrice, TotalTickets, TicketsSold FROM EVENT ORDER BY " + sortCol + " " + customOrder;
                } else if ("PRODUCT".equalsIgnoreCase(customTable)) {
                    String sortCol = "Stock".equalsIgnoreCase(customMetric) ? "Stock" : "Price";
                    sql = "SELECT ProductID, Name, Category, Price, Stock FROM PRODUCT ORDER BY " + sortCol + " " + customOrder;
                } else if ("LISTENER".equalsIgnoreCase(customTable)) {
                    sql = "SELECT ListenerID, Name, Gender FROM LISTENER ORDER BY Name " + customOrder;
                } else if ("TICKET_ORDER".equalsIgnoreCase(customTable)) {
                    sql = "SELECT OrderID, EventID, ListenerID, Quantity, TotalAmount FROM TICKET_ORDER ORDER BY TotalAmount " + customOrder;
                } else {
                    sql = "SELECT OrderID, ProductID, ListenerID, Quantity, TotalAmount FROM PRODUCT_ORDER ORDER BY TotalAmount " + customOrder;
                }
                break;

            default:
                sql = "SELECT ArtistID, Name FROM ARTIST";
        }
        return d1Service.getResults(d1Service.executeQuery(sql));
    }

    @GetMapping("/admin/reports")
    public String reportPage(@RequestParam String adminId,
                             @RequestParam(defaultValue = "topArtists") String filterType,
                             @RequestParam(required = false, defaultValue = "SONG") String customTable,
                             @RequestParam(required = false, defaultValue = "PlayCount") String customMetric,
                             @RequestParam(required = false, defaultValue = "DESC") String customOrder,
                             Model model) {
        model.addAttribute("adminId", adminId);
        try {
            var admin = d1Service.getResults(d1Service.executeQueryWithParams("SELECT Name FROM ADMIN WHERE AdminID = ?", List.of(adminId)));
            model.addAttribute("adminName", admin.isEmpty() ? "Admin" : admin.get(0).get("Name"));

            List<Map<String, Object>> reportData = fetchReportData(filterType, customTable, customMetric, customOrder);
            model.addAttribute("filterType", filterType);
            model.addAttribute("customTable", customTable);
            model.addAttribute("customMetric", customMetric);
            model.addAttribute("customOrder", customOrder);
            model.addAttribute("reportData", reportData);
            return "adminReports";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error evaluating table target matrix column pairing: " + e.getMessage());
            return "adminReportError";
        }
    }

    @GetMapping("/admin/reports/download")
    public ResponseEntity<?> downloadReport(@RequestParam String adminId,
                                            @RequestParam String filterType,
                                            @RequestParam String format,
                                            @RequestParam(required = false) String customTable,
                                            @RequestParam(required = false) String customMetric,
                                            @RequestParam(required = false) String customOrder) throws IOException {
        List<Map<String, Object>> data;
        try {
            data = fetchReportData(filterType, customTable, customMetric, customOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Engine configuration error: " + e.getMessage());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String filename = "Kyoto_" + filterType + "_Report";

        if ("pdf".equalsIgnoreCase(format)) {
            com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4.rotate());
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("Kyoto Music Platform - Management Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("Filter Configuration Type: " + filterType));
            document.add(new Paragraph(" "));

            if (!data.isEmpty()) {
                PdfPTable table = new PdfPTable(data.get(0).size());
                table.setWidthPercentage(100);
                for (String key : data.get(0).keySet()) table.addCell(key);
                for (Map<String, Object> row : data) {
                    for (Object val : row.values()) table.addCell(val != null ? val.toString() : "N/A");
                }
                document.add(table);
            }
            document.close();
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF).body(out.toByteArray());
        } else {
            XWPFDocument document = new XWPFDocument();
            XWPFParagraph title = document.createParagraph();
            XWPFRun run = title.createRun();
            run.setText("Kyoto Music Platform - Management Report Summary: " + filterType);
            run.setBold(true);

            if (!data.isEmpty()) {
                var firstRow = data.get(0);
                XWPFTable table = document.createTable(data.size() + 1, firstRow.size());
                int colIndex = 0;
                for (String key : firstRow.keySet()) table.getRow(0).getCell(colIndex++).setText(key);
                int rowIndex = 1;
                for (Map<String, Object> row : data) {
                    XWPFTableRow tableRow = table.getRow(rowIndex++);
                    int cIndex = 0;
                    for (Object val : row.values()) tableRow.getCell(cIndex++).setText(val != null ? val.toString() : "N/A");
                }
            }
            document.write(out);
            document.close();
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename + ".docx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")).body(out.toByteArray());
        }
    }
}