package com.kyoto.data.controller;

import com.kyoto.data.model.D1Response;
import com.kyoto.data.service.D1Service;
import com.kyoto.data.service.R2Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class AdminConroller {

    @Autowired
    private D1Service d1Service;

    @Autowired
    private R2Service musicService; // Inject R2 Service for file cleanup


    @GetMapping("/adminLogin")
    public String adminLoginPage() {
        return "adminLogin";
    }

    @PostMapping("/adminLogin")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        Model model) {
        // Query matches the ADMIN table schema
        String sql = "SELECT * FROM ADMIN WHERE Name = ? AND Password = ?";
        D1Response response = d1Service.executeQueryWithParams(sql, List.of(username, password));
        List<Map<String, Object>> results = d1Service.getResults(response);

        if (!results.isEmpty()) {
            String adminId = results.get(0).get("AdminID").toString();
            return "redirect:/adminDashboard?adminId=" + adminId;
        } else {
            model.addAttribute("error", "Invalid admin credentials");
            return "adminLogin";
        }
    }

    @GetMapping("/adminDashboard")
    public String adminDashboard(@RequestParam String adminId, Model model) {
        // 1. Fetch Admin Info
        var admin = d1Service.getResults(d1Service.executeQueryWithParams("SELECT Name FROM ADMIN WHERE AdminID = ?", List.of(adminId)));

        // 2. Fetch Stats: Total PlayCount from SONG table
        var stats = d1Service.getResults(d1Service.executeQuery("SELECT SUM(PlayCount) as TotalStreams, COUNT(*) as SongCount FROM SONG"));

        // 3. Fetch Management Lists
        var songs = d1Service.getResults(d1Service.executeQuery("SELECT * FROM SONG"));
        var artists = d1Service.getResults(d1Service.executeQuery("SELECT * FROM ARTIST"));
        var listeners = d1Service.getResults(d1Service.executeQuery("SELECT * FROM LISTENER"));

        model.addAttribute("adminName", admin.get(0).get("Name"));
        model.addAttribute("stats", stats.get(0));
        model.addAttribute("songs", songs);
        model.addAttribute("artists", artists);
        model.addAttribute("listeners", listeners);
        model.addAttribute("adminId", adminId);

        return "adminDashboard";
    }



    @PostMapping("/admin/addAdmin")
    public String addAdmin(@RequestParam String newAdminId, @RequestParam String name,
                           @RequestParam String password, @RequestParam String adminId) {
        d1Service.executeUpdateWithParams("INSERT INTO ADMIN (AdminID, Name, Password) VALUES (?, ?, ?)",
                List.of(newAdminId, name, password));
        return "redirect:/adminDashboard?adminId=" + adminId;
    }


    @PostMapping("/admin/deleteSong")
    public String deleteSong(@RequestParam String songId, @RequestParam String adminId) {
        // Get file keys from DB before deleting record
        String sql = "SELECT AudioFileURL, CoverArtURL FROM SONG WHERE SongID = ?";
        var results = d1Service.getResults(d1Service.executeQueryWithParams(sql, List.of(songId)));

        if (!results.isEmpty()) {
            Map<String, Object> song = results.get(0);
            // Delete from R2
            musicService.deleteSong((String) song.get("AudioFileURL"));
            musicService.deleteSong((String) song.get("CoverArtURL"));
        }

        // Delete database links
        d1Service.executeUpdateWithParams("DELETE FROM PLAYLIST_SONG WHERE SongID = ?", List.of(songId));
        d1Service.executeUpdateWithParams("DELETE FROM ALBUM_SONG WHERE SongID = ?", List.of(songId));
        d1Service.executeUpdateWithParams("DELETE FROM SONG WHERE SongID = ?", List.of(songId));

        return "redirect:/adminDashboard?adminId=" + adminId;
    }


    @PostMapping("/admin/deleteArtist")
    public String deleteArtist(@RequestParam String artistId, @RequestParam String adminId) {

        var songs = d1Service.getResults(d1Service.executeQueryWithParams("SELECT SongID, AudioFileURL, CoverArtURL FROM SONG WHERE ArtistID = ?", List.of(artistId)));
        for (Map<String, Object> song : songs) {
            musicService.deleteSong((String) song.get("AudioFileURL"));
            musicService.deleteSong((String) song.get("CoverArtURL"));
            d1Service.executeUpdateWithParams("DELETE FROM PLAYLIST_SONG WHERE SongID = ?", List.of(song.get("SongID")));
        }

        // Clear DB
        d1Service.executeUpdateWithParams("DELETE FROM ALBUM WHERE ArtistID = ?", List.of(artistId));
        d1Service.executeUpdateWithParams("DELETE FROM SONG WHERE ArtistID = ?", List.of(artistId));
        d1Service.executeUpdateWithParams("DELETE FROM ARTIST WHERE ArtistID = ?", List.of(artistId));

        return "redirect:/adminDashboard?adminId=" + adminId;
    }


    @PostMapping("/admin/deleteListener")
    public String deleteListener(@RequestParam String listenerId, @RequestParam String adminId) {
        // Delete playlist links first
        d1Service.executeUpdateWithParams("DELETE FROM PLAYLIST_SONG WHERE PlaylistID IN (SELECT PlaylistID FROM PLAYLIST WHERE ListenerID = ?)", List.of(listenerId));
        d1Service.executeUpdateWithParams("DELETE FROM PLAYLIST WHERE ListenerID = ?", List.of(listenerId));
        d1Service.executeUpdateWithParams("DELETE FROM LISTENER WHERE ListenerID = ?", List.of(listenerId));

        return "redirect:/adminDashboard?adminId=" + adminId;
    }

    @PostMapping("/admin/createListener")
    public String createListener(@RequestParam String listenerId,
                                 @RequestParam String name,
                                 @RequestParam String password,
                                 @RequestParam String gender,
                                 @RequestParam String adminId) {

        String sql = "INSERT INTO LISTENER (ListenerID, Name, Password, isPremium, AdminID, Gender) VALUES (?, ?, ?, ?, ?, ?)";
        d1Service.executeUpdateWithParams(sql, List.of(listenerId, name, password, 0, adminId, gender));

        return "redirect:/adminDashboard?adminId=" + adminId;
    }


    @PostMapping("/admin/createArtist")
    public String createArtist(@RequestParam String name,
                               @RequestParam String password,
                               @RequestParam String gender,
                               @RequestParam String adminId) {

        String artistId = "ART" + java.util.UUID.randomUUID().toString().substring(0, 7).toUpperCase();
        String sql = "INSERT INTO ARTIST (ArtistID, Name, Gender, Password) VALUES (?, ?, ?, ?)";
        d1Service.executeUpdateWithParams(sql, List.of(artistId, name, gender, password));

        return "redirect:/adminDashboard?adminId=" + adminId;
    }
}