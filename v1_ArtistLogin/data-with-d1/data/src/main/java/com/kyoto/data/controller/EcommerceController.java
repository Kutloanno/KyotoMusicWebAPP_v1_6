package com.kyoto.data.controller;

import com.kyoto.data.model.D1Response;
import com.kyoto.data.service.D1Service;
import com.kyoto.data.service.R2Service;
import com.kyoto.data.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Controller
public class EcommerceController {

    @Autowired
    private D1Service d1Service;

    @Autowired
    private R2Service r2Service;

    @Autowired
    private EmailService emailService;

    // Grab the Paystack key from your application.properties / Render environment variables
    @Value("${paystack.public.key:}")
    private String paystackPublicKey;

    // ─────────────────────────────────────────────────────────────────────────
    // ARTIST STORE ENDPOINTS
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/artist/store")
    public String artistStore(@RequestParam String artistId, Model model) {
        String sql = "SELECT * FROM PRODUCT WHERE ArtistID = ? ORDER BY CreatedAt DESC";
        D1Response res = d1Service.executeQueryWithParams(sql, List.of(artistId));
        model.addAttribute("products", d1Service.getResults(res));
        model.addAttribute("artistId", artistId);
        return "artist-store";
    }

    @PostMapping("/artist/store/add")
    public String addProduct(@RequestParam String artistId,
                             @RequestParam String name,
                             @RequestParam double price,
                             @RequestParam String category,
                             @RequestParam int stock,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) MultipartFile productImage,
                             Model model) {
        try {
            String productId = "PRD" + UUID.randomUUID().toString().substring(0, 7).toUpperCase();
            String imageKey = "";
            if (productImage != null && !productImage.isEmpty()) {
                imageKey = productId + "_" + productImage.getOriginalFilename();
                r2Service.uploadSong(imageKey, productImage.getInputStream(), productImage.getSize());
            }
            String sql = "INSERT INTO PRODUCT (ProductID, ArtistID, Name, Description, Price, Category, Stock, ImageURL, CreatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
            D1Response res = d1Service.executeUpdateWithParams(sql, List.of(productId, artistId, name, description != null ? description : "", price, category, stock, imageKey));
            return res.isSuccess() ? "redirect:/artist/store?artistId=" + artistId + "&success=Product+listed!" : "redirect:/artist/store?artistId=" + artistId + "&error=Failed";
        } catch (IOException e) {
            return "redirect:/artist/store?artistId=" + artistId + "&error=Image+upload+failed";
        }
    }

    @PostMapping("/artist/store/delete")
    public String deleteProduct(@RequestParam String productId, @RequestParam String artistId) {
        String getSql = "SELECT ImageURL FROM PRODUCT WHERE ProductID = ? AND ArtistID = ?";
        List<Map<String, Object>> rows = d1Service.getResults(d1Service.executeQueryWithParams(getSql, List.of(productId, artistId)));
        if (!rows.isEmpty()) {
            String imageKey = (String) rows.get(0).get("ImageURL");
            if (imageKey != null && !imageKey.isEmpty()) r2Service.deleteSong(imageKey);
        }
        d1Service.executeUpdateWithParams("DELETE FROM PRODUCT WHERE ProductID = ? AND ArtistID = ?", List.of(productId, artistId));
        return "redirect:/artist/store?artistId=" + artistId + "&success=Product+removed";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ARTIST EVENTS ENDPOINTS
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/artist/events")
    public String artistEvents(@RequestParam String artistId, Model model) {
        String eventsSql = "SELECT *, CASE WHEN EventDate < DATE('now') THEN 1 ELSE 0 END AS IsPast, SUBSTR(EventDate, 6, 2) AS MonthNum, SUBSTR(EventDate, 9, 2) AS DayStr FROM EVENT WHERE ArtistID = ? ORDER BY EventDate ASC";
        List<Map<String, Object>> events = d1Service.getResults(d1Service.executeQueryWithParams(eventsSql, List.of(artistId)));
        String[] months = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (Map<String, Object> ev : events) {
            try { ev.put("MonthStr", months[Integer.parseInt(ev.get("MonthNum").toString())]); } catch (Exception e) { ev.put("MonthStr", "---"); }
        }
        String ordersSql = "SELECT to2.*, e.Title AS EventTitle, l.Name AS BuyerName FROM TICKET_ORDER to2 JOIN EVENT e ON to2.EventID = e.EventID JOIN LISTENER l ON to2.ListenerID = l.ListenerID WHERE e.ArtistID = ? ORDER BY to2.PurchasedAt DESC";
        model.addAttribute("events", events);
        model.addAttribute("ticketOrders", d1Service.getResults(d1Service.executeQueryWithParams(ordersSql, List.of(artistId))));
        model.addAttribute("artistId", artistId);
        return "artist-events";
    }

    @PostMapping("/artist/events/add")
    public String addEvent(@RequestParam String artistId,
                           @RequestParam String title,
                           @RequestParam String venue,
                           @RequestParam String eventDate,
                           @RequestParam String eventTime,
                           @RequestParam double ticketPrice,
                           @RequestParam int totalTickets,
                           @RequestParam(required = false) String description,
                           @RequestParam(required = false) MultipartFile eventImage) {
        try {
            String eventId = "EVT" + UUID.randomUUID().toString().substring(0, 7).toUpperCase();
            String imageKey = "";
            if (eventImage != null && !eventImage.isEmpty()) {
                imageKey = eventId + "_" + eventImage.getOriginalFilename();
                r2Service.uploadSong(imageKey, eventImage.getInputStream(), eventImage.getSize());
            }
            String sql = "INSERT INTO EVENT (EventID, ArtistID, Title, Venue, EventDate, EventTime, TicketPrice, TotalTickets, TicketsSold, Description, ImageURL, CreatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, CURRENT_TIMESTAMP)";
            d1Service.executeUpdateWithParams(sql, List.of(eventId, artistId, title, venue, eventDate, eventTime, ticketPrice, totalTickets, description != null ? description : "", imageKey));
            return "redirect:/artist/events?artistId=" + artistId + "&success=Event+published!";
        } catch (IOException e) {
            return "redirect:/artist/events?artistId=" + artistId + "&error=Image+upload+failed";
        }
    }

    @PostMapping("/artist/events/delete")
    public String deleteEvent(@RequestParam String eventId, @RequestParam String artistId) {
        d1Service.executeUpdateWithParams("DELETE FROM TICKET_ORDER WHERE EventID = ?", List.of(eventId));
        List<Map<String, Object>> rows = d1Service.getResults(d1Service.executeQueryWithParams("SELECT ImageURL FROM EVENT WHERE EventID = ? AND ArtistID = ?", List.of(eventId, artistId)));
        if (!rows.isEmpty()) {
            String imgKey = (String) rows.get(0).get("ImageURL");
            if (imgKey != null && !imgKey.isEmpty()) r2Service.deleteSong(imgKey);
        }
        d1Service.executeUpdateWithParams("DELETE FROM EVENT WHERE EventID = ? AND ArtistID = ?", List.of(eventId, artistId));
        return "redirect:/artist/events?artistId=" + artistId + "&success=Event+cancelled";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LISTENER / USER STORE & EVENTS VIEW
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/store")
    public String userStore(@RequestParam String listenerId, @RequestParam(required = false) String category, Model model) {
        String sql;
        D1Response res;
        if (category != null && !category.isEmpty()) {
            sql = "SELECT p.*, a.Name AS ArtistName FROM PRODUCT p JOIN ARTIST a ON p.ArtistID = a.ArtistID WHERE p.Category = ? AND p.Stock > 0 ORDER BY p.CreatedAt DESC";
            res = d1Service.executeQueryWithParams(sql, List.of(category));
        } else {
            sql = "SELECT p.*, a.Name AS ArtistName FROM PRODUCT p JOIN ARTIST a ON p.ArtistID = a.ArtistID WHERE p.Stock > 0 ORDER BY p.CreatedAt DESC";
            res = d1Service.executeQuery(sql);
        }
        model.addAttribute("products", d1Service.getResults(res));
        model.addAttribute("listenerId", listenerId);
        model.addAttribute("category", category);
        return "user-store";
    }

    @GetMapping("/events")
    public String userEvents(@RequestParam String listenerId, Model model) {
        String eventsSql = "SELECT e.*, SUBSTR(e.EventDate, 6, 2) AS MonthNum, SUBSTR(e.EventDate, 9, 2) AS DayStr, a.Name AS ArtistName FROM EVENT e JOIN ARTIST a ON e.ArtistID = a.ArtistID ORDER BY e.EventDate ASC";
        List<Map<String, Object>> events = d1Service.getResults(d1Service.executeQuery(eventsSql));
        String[] months = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (Map<String, Object> ev : events) {
            try { ev.put("MonthStr", months[Integer.parseInt(ev.get("MonthNum").toString())]); } catch (Exception e) { ev.put("MonthStr", "---"); }
        }
        String myTicketsSql = "SELECT to2.*, e.Title AS EventTitle, e.EventDate, e.Venue FROM TICKET_ORDER to2 JOIN EVENT e ON to2.EventID = e.EventID WHERE to2.ListenerID = ? ORDER BY to2.PurchasedAt DESC";
        model.addAttribute("events", events);
        model.addAttribute("myTickets", d1Service.getResults(d1Service.executeQueryWithParams(myTicketsSql, List.of(listenerId))));
        model.addAttribute("listenerId", listenerId);
        return "user-events";
    }

    @PostMapping("/events/cancelTicket")
    public String cancelTicket(@RequestParam String orderId, @RequestParam String listenerId) {
        String checkSql = "SELECT EventID, Quantity FROM TICKET_ORDER WHERE OrderID = ? AND ListenerID = ?";
        List<Map<String, Object>> orderDetails = d1Service.getResults(d1Service.executeQueryWithParams(checkSql, List.of(orderId, listenerId)));

        if (!orderDetails.isEmpty()) {
            String eventId = (String) orderDetails.get(0).get("EventID");
            int quantity = ((Number) orderDetails.get(0).get("Quantity")).intValue();
            d1Service.executeUpdateWithParams("DELETE FROM TICKET_ORDER WHERE OrderID = ?", List.of(orderId));
            d1Service.executeUpdateWithParams("UPDATE EVENT SET TicketsSold = TicketsSold - ? WHERE EventID = ?", List.of(quantity, eventId));
            return "redirect:/events?listenerId=" + listenerId + "&success=Ticket+successfully+cancelled+and+refunded.";
        }
        return "redirect:/events?listenerId=" + listenerId + "&error=Ticket+not+found+or+unauthorized.";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UNIFIED SHOPPING CART ENDPOINTS (PRODUCTS + TICKETS)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam String productId, @RequestParam String listenerId) {
        String checkSql = "SELECT CartItemID, Quantity FROM CART_ITEM WHERE ListenerID = ? AND ProductID = ?";
        List<Map<String, Object>> existing = d1Service.getResults(d1Service.executeQueryWithParams(checkSql, List.of(listenerId, productId)));

        if (!existing.isEmpty()) {
            String cartItemId = (String) existing.get(0).get("CartItemID");
            d1Service.executeUpdateWithParams("UPDATE CART_ITEM SET Quantity = Quantity + 1 WHERE CartItemID = ?", List.of(cartItemId));
        } else {
            String cartItemId = "CRT" + UUID.randomUUID().toString().substring(0, 7).toUpperCase();
            d1Service.executeUpdateWithParams("INSERT INTO CART_ITEM (CartItemID, ListenerID, ProductID, Quantity) VALUES (?, ?, ?, 1)", List.of(cartItemId, listenerId, productId));
        }
        return "redirect:/store?listenerId=" + listenerId + "&success=Added+to+cart!";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam String cartItemId, @RequestParam String listenerId) {
        // Find the exact cart item and delete it from the Cloudflare database
        d1Service.executeUpdateWithParams("DELETE FROM CART_ITEM WHERE CartItemID = ?", List.of(cartItemId));

        // Refresh the cart page automatically
        return "redirect:/cart?listenerId=" + listenerId + "&success=Item+removed";
    }

    @PostMapping("/cart/addTicket")
    public String addTicketToCart(@RequestParam String eventId, @RequestParam String listenerId, @RequestParam int quantity) {
        String checkSql = "SELECT CartItemID, Quantity FROM CART_ITEM WHERE ListenerID = ? AND EventID = ?";
        List<Map<String, Object>> existing = d1Service.getResults(d1Service.executeQueryWithParams(checkSql, List.of(listenerId, eventId)));

        if (!existing.isEmpty()) {
            String cartItemId = (String) existing.get(0).get("CartItemID");
            d1Service.executeUpdateWithParams("UPDATE CART_ITEM SET Quantity = Quantity + ? WHERE CartItemID = ?", List.of(quantity, cartItemId));
        } else {
            String cartItemId = "CRT" + UUID.randomUUID().toString().substring(0, 7).toUpperCase();
            d1Service.executeUpdateWithParams("INSERT INTO CART_ITEM (CartItemID, ListenerID, EventID, Quantity) VALUES (?, ?, ?, ?)", List.of(cartItemId, listenerId, eventId, quantity));
        }
        return "redirect:/events?listenerId=" + listenerId + "&success=Tickets+added+to+cart!";
    }

    @GetMapping("/cart")
    public String viewCart(@RequestParam String listenerId, Model model) {
        String pSql = "SELECT c.CartItemID, c.Quantity, p.ProductID, p.Name, p.Price, p.Stock, p.ImageURL, a.Name AS ArtistName FROM CART_ITEM c JOIN PRODUCT p ON c.ProductID = p.ProductID JOIN ARTIST a ON p.ArtistID = a.ArtistID WHERE c.ListenerID = ? AND c.ProductID IS NOT NULL";
        List<Map<String, Object>> cartProducts = d1Service.getResults(d1Service.executeQueryWithParams(pSql, List.of(listenerId)));

        String tSql = "SELECT c.CartItemID, c.Quantity, e.EventID, e.Title AS Name, e.TicketPrice AS Price, (e.TotalTickets - e.TicketsSold) AS Stock, e.ImageURL, a.Name AS ArtistName, e.EventDate FROM CART_ITEM c JOIN EVENT e ON c.EventID = e.EventID JOIN ARTIST a ON e.ArtistID = a.ArtistID WHERE c.ListenerID = ? AND c.EventID IS NOT NULL";
        List<Map<String, Object>> cartTickets = d1Service.getResults(d1Service.executeQueryWithParams(tSql, List.of(listenerId)));

        double subtotal = 0;
        for(Map<String, Object> item : cartProducts) subtotal += ((Number) item.get("Price")).doubleValue() * ((Number) item.get("Quantity")).intValue();
        for(Map<String, Object> item : cartTickets) subtotal += ((Number) item.get("Price")).doubleValue() * ((Number) item.get("Quantity")).intValue();

        model.addAttribute("cartProducts", cartProducts);
        model.addAttribute("cartTickets", cartTickets);
        model.addAttribute("listenerId", listenerId);
        model.addAttribute("subtotal", String.format("%.2f", subtotal));
        model.addAttribute("isEmpty", cartProducts.isEmpty() && cartTickets.isEmpty());
        return "cart";
    }

    @GetMapping("/cart/checkout")
    public String showCartCheckout(@RequestParam String listenerId, Model model) {
        String pSql = "SELECT c.Quantity, p.Price, p.Name FROM CART_ITEM c JOIN PRODUCT p ON c.ProductID = p.ProductID WHERE c.ListenerID = ?";
        List<Map<String, Object>> pItems = d1Service.getResults(d1Service.executeQueryWithParams(pSql, List.of(listenerId)));

        String tSql = "SELECT c.Quantity, e.TicketPrice AS Price, e.Title AS Name FROM CART_ITEM c JOIN EVENT e ON c.EventID = e.EventID WHERE c.ListenerID = ?";
        List<Map<String, Object>> tItems = d1Service.getResults(d1Service.executeQueryWithParams(tSql, List.of(listenerId)));

        if (pItems.isEmpty() && tItems.isEmpty()) return "redirect:/store?listenerId=" + listenerId;

        double subtotal = 0;
        StringBuilder itemList = new StringBuilder();

        for(Map<String, Object> item : pItems) {
            subtotal += ((Number) item.get("Price")).doubleValue() * ((Number) item.get("Quantity")).intValue();
            itemList.append(item.get("Name")).append(" (x").append(item.get("Quantity")).append("), ");
        }

        for(Map<String, Object> item : tItems) {
            subtotal += ((Number) item.get("Price")).doubleValue() * ((Number) item.get("Quantity")).intValue();
            itemList.append("🎟️ ").append(item.get("Name")).append(" (x").append(item.get("Quantity")).append("), ");
        }

        double feeTotal = 0;
        if (!pItems.isEmpty()) feeTotal += 5.00;
        if (!tItems.isEmpty()) feeTotal += 2.50;

        double finalTotal = subtotal + feeTotal;
        String itemSummary = itemList.toString();
        if(itemSummary.endsWith(", ")) itemSummary = itemSummary.substring(0, itemSummary.length() - 2);

        // Convert final total to smallest currency unit (cents) for Paystack
        long paystackAmountInCents = Math.round(finalTotal * 100);

        model.addAttribute("listenerId", listenerId);
        model.addAttribute("itemSummary", itemSummary);
        model.addAttribute("subtotal", String.format("%.2f", subtotal));
        model.addAttribute("feeTotal", String.format("%.2f", feeTotal));
        model.addAttribute("finalTotal", String.format("%.2f", finalTotal));

        // Pass Paystack details to the frontend
        model.addAttribute("paystackPublicKey", paystackPublicKey);
        model.addAttribute("paystackAmount", paystackAmountInCents);

        return "cart-checkout";
    }

    // Notice we added @RequestParam String paystackReference to grab the success ID!
    @PostMapping("/cart/processPayment")
    public String processCartPayment(@RequestParam String listenerId, @RequestParam String email, @RequestParam(required = false) String paystackReference) {

        // 1. Fetch & Verify Merch Stock
        String pSql = "SELECT c.Quantity as CartQty, p.ProductID, p.Name, p.Price, p.Stock, a.Email AS ArtistEmail FROM CART_ITEM c JOIN PRODUCT p ON c.ProductID = p.ProductID JOIN ARTIST a ON p.ArtistID = a.ArtistID WHERE c.ListenerID = ?";
        List<Map<String, Object>> pItems = d1Service.getResults(d1Service.executeQueryWithParams(pSql, List.of(listenerId)));
        for(Map<String, Object> item : pItems) {
            int stock = ((Number) item.get("Stock")).intValue();
            int qty = ((Number) item.get("CartQty")).intValue();
            if (stock < qty) return "redirect:/cart?listenerId=" + listenerId + "&error=Not+enough+stock+for+" + item.get("Name");
        }

        // 2. Fetch & Verify Ticket Stock
        String tSql = "SELECT c.Quantity as CartQty, e.EventID, e.Title AS Name, e.TicketPrice AS Price, (e.TotalTickets - e.TicketsSold) AS Stock, a.Email AS ArtistEmail FROM CART_ITEM c JOIN EVENT e ON c.EventID = e.EventID JOIN ARTIST a ON e.ArtistID = a.ArtistID WHERE c.ListenerID = ?";
        List<Map<String, Object>> tItems = d1Service.getResults(d1Service.executeQueryWithParams(tSql, List.of(listenerId)));
        for(Map<String, Object> item : tItems) {
            int stock = ((Number) item.get("Stock")).intValue();
            int qty = ((Number) item.get("CartQty")).intValue();
            if (stock < qty) return "redirect:/cart?listenerId=" + listenerId + "&error=Not+enough+tickets+for+" + item.get("Name");
        }

        String receiptId = "RCPT" + UUID.randomUUID().toString().substring(0, 7).toUpperCase();
        double subtotal = 0;
        StringBuilder receiptItems = new StringBuilder();

        // Optional: Append the Paystack Reference to the receipt if it exists
        if (paystackReference != null && !paystackReference.isEmpty()) {
            receiptItems.append("Payment Ref: ").append(paystackReference).append("\n\n");
        }

        // 3. Process Merch
        for(Map<String, Object> item : pItems) {
            String productId = (String) item.get("ProductID");
            int qty = ((Number) item.get("CartQty")).intValue();
            double price = ((Number) item.get("Price")).doubleValue();
            double itemTotal = price * qty;
            subtotal += itemTotal;
            String name = (String) item.get("Name");
            String artistEmail = (String) item.get("ArtistEmail");

            receiptItems.append("- 📦 ").append(name).append(" (x").append(qty).append(") : $").append(String.format("%.2f", itemTotal)).append("\n");

            String lineOrderId = "ORD" + UUID.randomUUID().toString().substring(0, 7).toUpperCase();
            d1Service.executeUpdateWithParams("INSERT INTO PRODUCT_ORDER (OrderID, ProductID, ListenerID, Quantity, TotalAmount, PurchasedAt) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)", List.of(lineOrderId, productId, listenerId, qty, itemTotal));
            d1Service.executeUpdateWithParams("UPDATE PRODUCT SET Stock = Stock - ? WHERE ProductID = ?", List.of(qty, productId));
            if (artistEmail != null && !artistEmail.trim().isEmpty()) { try { emailService.sendArtistNotification(artistEmail, name + " (x" + qty + ")", itemTotal); } catch (Exception ignored) {} }
        }

        // 4. Process Tickets
        for(Map<String, Object> item : tItems) {
            String eventId = (String) item.get("EventID");
            int qty = ((Number) item.get("CartQty")).intValue();
            double price = ((Number) item.get("Price")).doubleValue();
            double itemTotal = price * qty;
            subtotal += itemTotal;
            String name = (String) item.get("Name");
            String artistEmail = (String) item.get("ArtistEmail");

            String lineOrderId = "TKT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            receiptItems.append("- 🎟️ ").append(name).append(" (x").append(qty).append(") : $").append(String.format("%.2f", itemTotal))
                    .append("\n     ↳ DOOR CODE: ").append(lineOrderId).append("\n");

            d1Service.executeUpdateWithParams("INSERT INTO TICKET_ORDER (OrderID, EventID, ListenerID, Quantity, TotalAmount, TicketCode, PurchasedAt) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)", List.of(lineOrderId, eventId, listenerId, qty, itemTotal, lineOrderId));
            d1Service.executeUpdateWithParams("UPDATE EVENT SET TicketsSold = TicketsSold + ? WHERE EventID = ?", List.of(qty, eventId));
            if (artistEmail != null && !artistEmail.trim().isEmpty()) { try { emailService.sendArtistTicketNotification(artistEmail, name, qty, itemTotal); } catch (Exception ignored) {} }
        }

        // 5. Finalize Math & Email
        double feeTotal = 0;
        if (!pItems.isEmpty()) feeTotal += 5.00;
        if (!tItems.isEmpty()) feeTotal += 2.50;
        double finalTotal = subtotal + feeTotal;

        d1Service.executeUpdateWithParams("DELETE FROM CART_ITEM WHERE ListenerID = ?", List.of(listenerId));

        try {
            emailService.sendCartOrderConfirmation(email, receiptId, receiptItems.toString(), finalTotal);
        } catch (Exception e) { System.out.println("Mail error: " + e.getMessage()); }

        return "redirect:/store?listenerId=" + listenerId + "&success=Order+placed!+Receipt+and+tickets+sent+to+" + email;
    }
}