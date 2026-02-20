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

@Controller
public class ListinerController {

    @Autowired
    private D1Service d1Service;

    private R2Service musicService;

    @PostMapping("/userLogin")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        Model model) {

        String sql = "SELECT * FROM LISTENER WHERE ListenerID = ? AND Password = ?";
        D1Response response = d1Service.executeQueryWithParams(sql, List.of(username, password));
        List<Map<String, Object>> results = d1Service.getResults(response);

        if (!results.isEmpty()) {
            Map<String, Object> listener = results.get(0);
            String listenerId = (String) listener.get("ListenerID");
            return "redirect:/userDashBoard?listenerId=" + listenerId;
        } else {
            model.addAttribute("error", "Invalid username or password");
            return "userLogin";
        }
    }

    @GetMapping("/userDashBoard")
    public String userDashBoard(@RequestParam String listenerId, Model model) {
        String sql = "SELECT * FROM LISTENER WHERE ListenerID = ?";
        D1Response response = d1Service.executeQueryWithParams(sql, List.of(listenerId));
        List<Map<String, Object>> results = d1Service.getResults(response);

        if (results.isEmpty()) return "redirect:/userLogin";

        String plSql = "SELECT * FROM PLAYLIST WHERE ListenerID = ?";
        List<Map<String, Object>> playlists = d1Service.getResults(d1Service.executeQueryWithParams(plSql, List.of(listenerId)));

        String snSql = "SELECT s.*, a.Name as ArtistName " +
                "FROM SONG s " +
                "LEFT JOIN ARTIST a ON s.ArtistID = a.ArtistID " +
                "ORDER BY RANDOM() LIMIT 3";

        D1Response songs = d1Service.executeQuery(snSql);
        List<Map<String, Object>> randomSongs = d1Service.getResults(songs);

        model.addAttribute("listener", results.get(0));
        model.addAttribute("listenerId", listenerId);
        model.addAttribute("playlists", playlists);
        model.addAttribute("randomSongs", randomSongs);

        return "userDashBoard";
    }

    @PostMapping("/createPlaylist")
    public String createPlaylist(@RequestParam String name, @RequestParam int isPublic, @RequestParam String listenerId) {
        String id = "PL" + java.util.UUID.randomUUID().toString().substring(0, 7).toUpperCase();
        String sql = "INSERT INTO PLAYLIST (PlaylistID, Name, ListenerID, isPublic) VALUES (?, ?, ?, ?)";
        d1Service.executeUpdateWithParams(sql, List.of(id, name, listenerId, isPublic));
        return "redirect:/userDashBoard?listenerId=" + listenerId;
    }

    @PostMapping("/addToPlaylist")
    public String addToPlaylist(@RequestParam String playlistId, @RequestParam String songId, @RequestParam String listenerId) {
        try {
            String checkSql = "SELECT * FROM PLAYLIST_SONG WHERE PlaylistID = ? AND SongID = ?";
            D1Response checkRes = d1Service.executeQueryWithParams(checkSql, List.of(playlistId, songId));
            List<Map<String, Object>> existing = d1Service.getResults(checkRes);

            if (!existing.isEmpty()) {
                return "redirect:/userDashBoard?listenerId=" + listenerId + "&error=Song+is+already+in+this+playlist";
            }

            String countSql = "SELECT COUNT(*) as count FROM PLAYLIST_SONG WHERE PlaylistID = ?";
            List<Map<String, Object>> res = d1Service.getResults(d1Service.executeQueryWithParams(countSql, List.of(playlistId)));
            int pos = res.isEmpty() ? 1 : ((Number) res.get(0).get("count")).intValue() + 1;

            String sql = "INSERT INTO PLAYLIST_SONG (PlaylistID, SongID, Position) VALUES (?, ?, ?)";
            d1Service.executeUpdateWithParams(sql, List.of(playlistId, songId, pos));

            return "redirect:/userDashBoard?listenerId=" + listenerId + "&success=Song+added+successfully!";

        } catch (Exception e) {
            return "redirect:/userDashBoard?listenerId=" + listenerId + "&error=Failed+to+add+song";
        }
    }

    @GetMapping("/search")
    public String search(@RequestParam String query, @RequestParam String listenerId, Model model) {
        try {
            String searchPattern = "%" + query + "%";

            String songSql = "SELECT s.*, a.Name as ArtistName FROM SONG s " +
                    "JOIN ARTIST a ON s.ArtistID = a.ArtistID " +
                    "WHERE s.Title LIKE ? OR a.Name LIKE ?";
            List<Map<String, Object>> songs = d1Service.getResults(d1Service.executeQueryWithParams(songSql, List.of(searchPattern, searchPattern)));

            String playlistSearchSql = "SELECT * FROM PLAYLIST WHERE Name LIKE ? AND ListenerID != ?";
            List<Map<String, Object>> foundPlaylists = d1Service.getResults(d1Service.executeQueryWithParams(playlistSearchSql, List.of(searchPattern, listenerId)));

            String userPlSql = "SELECT * FROM PLAYLIST WHERE ListenerID = ?";
            List<Map<String, Object>> myPlaylists = d1Service.getResults(d1Service.executeQueryWithParams(userPlSql, List.of(listenerId)));

            model.addAttribute("songs", songs);
            model.addAttribute("publicPlaylists", foundPlaylists);
            model.addAttribute("playlists", myPlaylists);
            model.addAttribute("query", query);
            model.addAttribute("listenerId", listenerId);

            return "searchResults";
        } catch (Exception e) {
            return "userDashBoard";
        }
    }

    @GetMapping("/viewPlaylist")
    public String viewPlaylist(@RequestParam String playlistId, @RequestParam String listenerId, Model model) {

        String plSql = "SELECT Name FROM PLAYLIST WHERE PlaylistID = ?";
        D1Response plRes = d1Service.executeQueryWithParams(plSql, List.of(playlistId));
        List<Map<String, Object>> plRows = d1Service.getResults(plRes);
        String playlistName = plRows.isEmpty() ? "My Playlist" : (String) plRows.get(0).get("Name");

        String sql = "SELECT s.*, a.Name as ArtistName " +
                "FROM PLAYLIST_SONG ps " +
                "JOIN SONG s ON ps.SongID = s.SongID " +
                "JOIN ARTIST a ON s.ArtistID = a.ArtistID " +
                "WHERE ps.PlaylistID = ? " +
                "ORDER BY ps.Position ASC";

        D1Response response = d1Service.executeQueryWithParams(sql, List.of(playlistId));
        List<Map<String, Object>> playlistSongs = d1Service.getResults(response);

        model.addAttribute("playlistName", playlistName);
        model.addAttribute("songs", playlistSongs);
        model.addAttribute("listenerId", listenerId);
        model.addAttribute("playlistId", playlistId);

        return "viewPlaylist";
    }

    @PostMapping("/registerUser")
    public String registerUser(@RequestParam String name,
                               @RequestParam String username,
                               @RequestParam String password,
                               @RequestParam String gender,
                               Model model) {
        try {
            String checkSql = "SELECT * FROM LISTENER WHERE ListenerID = ?";
            D1Response checkRes = d1Service.executeQueryWithParams(checkSql, List.of(username));

            if (!d1Service.getResults(checkRes).isEmpty()) {
                model.addAttribute("error", "Username already taken!");
                return "userSignup";
            }

            String insertSql = "INSERT INTO LISTENER (ListenerID, Name, Password,isPremium,AdminID,Gender) VALUES (?, ?, ?,?,?,?)";
            d1Service.executeUpdateWithParams(insertSql, List.of(username, name, password, 0, "ADM001", gender));

            return "redirect:/userLogin?success=AccountCreated";

        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "userSignup";
        }
    }

    @PostMapping("/followPlaylist")
    public String followPlaylist(@RequestParam String playlistId, @RequestParam String listenerId) {
        try {
            String newPlId = "PL" + System.currentTimeMillis();
            String getPlName = "SELECT Name FROM PLAYLIST WHERE PlaylistID = ?";
            Map<String, Object> oldPl = d1Service.getResults(d1Service.executeQueryWithParams(getPlName, List.of(playlistId))).get(0);

            String createSql = "INSERT INTO PLAYLIST (PlaylistID, Name, ListenerID) VALUES (?, ?, ?)";
            d1Service.executeUpdateWithParams(createSql, List.of(newPlId, oldPl.get("Name") + " (Followed)", listenerId));

            String copySongsSql = "INSERT INTO PLAYLIST_SONG (PlaylistID, SongID, Position) " +
                    "SELECT ?, SongID, Position FROM PLAYLIST_SONG WHERE PlaylistID = ?";
            d1Service.executeUpdateWithParams(copySongsSql, List.of(newPlId, playlistId));

            return "redirect:/userDashBoard?listenerId=" + listenerId;
        } catch (Exception e) {
            return "redirect:/userDashBoard?listenerId=" + listenerId + "&error=copy_failed";
        }
    }

    @GetMapping("/user/settings")
    public String showUserSettings(@RequestParam String listenerId, Model model) {
        String sql = "SELECT * FROM LISTENER WHERE ListenerID = ?";
        D1Response response = d1Service.executeQueryWithParams(sql, List.of(listenerId));
        model.addAttribute("user", d1Service.getResults(response).get(0));
        model.addAttribute("listenerId", listenerId);
        return "user-settings";
    }

    @PostMapping("/user/update")
    public String updateUser(@RequestParam String listenerId,
                             @RequestParam String name,
                             @RequestParam String password) {
        String sql = "UPDATE LISTENER SET Name = ?, Password = ? WHERE ListenerID = ?";
        d1Service.executeUpdateWithParams(sql, List.of(name, password, listenerId));
        return "redirect:/userDashBoard?listenerId=" + listenerId + "&updated=true";
    }

    @PostMapping("/user/delete")
    public String deleteUser(@RequestParam String listenerId) {

        String findPlaylistsSql = "SELECT PlaylistID FROM PLAYLIST WHERE ListenerID = ?";
        D1Response playlistRes = d1Service.executeQueryWithParams(findPlaylistsSql, List.of(listenerId));
        List<Map<String, Object>> playlists = d1Service.getResults(playlistRes);

        for (Map<String, Object> pl : playlists) {
            String plId = (String) pl.get("PlaylistID");
            d1Service.executeUpdateWithParams("DELETE FROM PLAYLIST_SONG WHERE PlaylistID = ?", List.of(plId));
        }

        d1Service.executeUpdateWithParams("DELETE FROM PLAYLIST WHERE ListenerID = ?", List.of(listenerId));
        d1Service.executeUpdateWithParams("DELETE FROM LISTENER WHERE ListenerID = ?", List.of(listenerId));

        return "redirect:/userLogin?deleted=true";
    }
}