package com.kyoto.data.controller;

import com.kyoto.data.model.D1Response;
import com.kyoto.data.service.D1Service;
import com.kyoto.data.service.R2Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class ArtistController {

    @Autowired
    private D1Service d1Service;

    private final R2Service musicService;

    public ArtistController(R2Service musicService) {
        this.musicService = musicService;
    }

    @PostMapping("/artistlogin")
    public String artistLogin(@RequestParam String username,
                              @RequestParam String password,
                              Model model) {

        String sql = "SELECT * FROM ARTIST WHERE Name =? AND Password =?";
        D1Response response = d1Service.executeQueryWithParams(sql, List.of(username, password));
        List<Map<String, Object>> results = d1Service.getResults(response);

        if (!results.isEmpty()) {
            Map<String, Object> artist = results.get(0);
            String artistId = (String) artist.get("ArtistID");
            return "redirect:/artistdashboard?artistId=" + artistId;
        } else {
            model.addAttribute("error", "Invalid username or password");
            return "artistlogin";
        }
    }

    @GetMapping("/artistdashboard")
    public String artistDashboard(@RequestParam String artistId, Model model) {
        String artistSql = "SELECT * FROM ARTIST WHERE ArtistID = ?";
        D1Response artistResponse = d1Service.executeQueryWithParams(artistSql, List.of(artistId));
        List<Map<String, Object>> artistResults = d1Service.getResults(artistResponse);

        if (artistResults.isEmpty()) {
            return "redirect:/artistlogin";
        }

        String artistName = (String) artistResults.get(0).get("Name");

        String albumsSql = "SELECT * FROM ALBUM WHERE ArtistID = ? ORDER BY ReleaseDate DESC";
        D1Response albumsResponse = d1Service.executeQueryWithParams(albumsSql, List.of(artistId));
        List<Map<String, Object>> albums = d1Service.getResults(albumsResponse);

        String songsSql = "SELECT * FROM SONG WHERE ArtistID = ? ORDER BY ReleaseDate DESC";
        D1Response songsResponse = d1Service.executeQueryWithParams(songsSql, List.of(artistId));
        List<Map<String, Object>> songs = d1Service.getResults(songsResponse);

        int totalPlays = 0;
        int totalDownloads = 0;
        for (Map<String, Object> song : songs) {
            totalPlays += song.get("PlayCount") != null ? ((Number) song.get("PlayCount")).intValue() : 0;
            totalDownloads += song.get("DownloadCount") != null ? ((Number) song.get("DownloadCount")).intValue() : 0;
        }

        model.addAttribute("artistName", artistName);
        model.addAttribute("artistId", artistId);
        model.addAttribute("songs", songs);
        model.addAttribute("albums", albums);
        model.addAttribute("totalSongs", songs.size());
        model.addAttribute("totalPlays", totalPlays);
        model.addAttribute("totalDownloads", totalDownloads);

        return "artistdashboard";
    }

    @PostMapping("/artistsignup")
    public String artistSignup(@RequestParam String name,
                               @RequestParam String email,
                               @RequestParam String gender,
                               @RequestParam String password,
                               @RequestParam String confirmPassword,
                               Model model) {

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match");
            return "artistsignup";
        }

        String artistId = "ART" + UUID.randomUUID().toString().substring(0, 7).toUpperCase();

        String sql = "INSERT INTO ARTIST (ArtistID, Name, Email, Gender, Password, AdminID) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            D1Response response = d1Service.executeUpdateWithParams(sql, List.of(artistId, name, email, gender, password, "ADM001"));
            if (response.isSuccess()) {
                model.addAttribute("success", "Account created successfully! Please login.");
                return "artistlogin";
            } else {
                model.addAttribute("error", "Failed to create account.");
                return "artistsignup";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error: " + e.getMessage());
            return "artistsignup";
        }
    }

    @GetMapping("/addSong")
    public String showAddSongPage(@RequestParam String artistId,
                                  @RequestParam(required = false) String newGenre,
                                  Model model) {

        if (newGenre != null && !newGenre.trim().isEmpty()) {
            try {
                String genreId = "GEN" + java.util.UUID.randomUUID().toString().substring(0, 5).toUpperCase();
                String insertSql = "INSERT INTO GENRE (GenreID, GenreName) VALUES (?, ?)";
                d1Service.executeUpdateWithParams(insertSql, java.util.List.of(genreId, newGenre));
                model.addAttribute("success", "Genre '" + newGenre + "' added!");
            } catch (Exception e) {
                model.addAttribute("error", "Error adding genre: " + e.getMessage());
            }
        }

        String genreSql = "SELECT * FROM GENRE";
        D1Response genreResponse = d1Service.executeQuery(genreSql);
        model.addAttribute("genres", d1Service.getResults(genreResponse));

        String albumSql = "SELECT * FROM ALBUM WHERE ArtistID = ?";
        D1Response albumResponse = d1Service.executeQueryWithParams(albumSql, java.util.List.of(artistId));
        model.addAttribute("albums", d1Service.getResults(albumResponse));

        model.addAttribute("artistId", artistId);
        return "addsong";
    }

    @PostMapping("/uploadSong")
    public String uploadSong(@RequestParam String title,
                             @RequestParam int duration,
                             @RequestParam String genreId,
                             @RequestParam String artistId,
                             @RequestParam(required = false) String albumOption,
                             @RequestParam(required = false) String existingAlbumId,
                             @RequestParam(required = false) String newAlbumTitle,
                             @RequestParam(required = false) MultipartFile audioFile,
                             @RequestParam(required = false) MultipartFile coverArt,
                             Model model) {

        String songId = "SNG" + UUID.randomUUID().toString().substring(0, 7).toUpperCase();

        String audioKey = (audioFile != null && !audioFile.isEmpty()) ?
                songId + "_" + audioFile.getOriginalFilename() : "";
        String coverKey = (coverArt != null && !coverArt.isEmpty()) ?
                songId + "_cover_" + coverArt.getOriginalFilename() : "";

        try {
            if (!audioKey.isEmpty()) musicService.uploadSong(audioKey, audioFile.getInputStream(), audioFile.getSize());
            if (!coverKey.isEmpty()) musicService.uploadSong(coverKey, coverArt.getInputStream(), coverArt.getSize());
        } catch (IOException e) {
            return "redirect:/addSong?artistId=" + artistId + "&error=UploadFailed";
        }

        String songSql = "INSERT INTO SONG (SongID, Title, Duration, GenreID, ArtistID, ReleaseDate, AudioFileURL, CoverArtURL) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?)";
        d1Service.executeUpdateWithParams(songSql, List.of(songId, title, duration, genreId, artistId, audioKey, coverKey));

        if ("new".equals(albumOption) && newAlbumTitle != null && !newAlbumTitle.isEmpty()) {
            String targetAlbumId = "ALB" + UUID.randomUUID().toString().substring(0, 7).toUpperCase();
            String albumSql = "INSERT INTO ALBUM (AlbumID, Title, ArtistID, ReleaseDate, CoverArtURL) VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?)";
            d1Service.executeUpdateWithParams(albumSql, List.of(targetAlbumId, newAlbumTitle, artistId, coverKey));

            String linkSql = "INSERT INTO ALBUM_SONG (AlbumID, SongID, TrackNumber) VALUES (?, ?, 1)";
            d1Service.executeUpdateWithParams(linkSql, List.of(targetAlbumId, songId));

        } else if ("existing".equals(albumOption) && existingAlbumId != null && !existingAlbumId.isEmpty()) {
            String countSql = "SELECT COUNT(*) as count FROM ALBUM_SONG WHERE AlbumID = ?";
            D1Response countRes = d1Service.executeQueryWithParams(countSql, List.of(existingAlbumId));

            int trackNum = 1;
            List<Map<String, Object>> results = d1Service.getResults(countRes);
            if (results != null && !results.isEmpty() && results.get(0).get("count") != null) {
                trackNum = Integer.parseInt(results.get(0).get("count").toString()) + 1;
            }

            String linkSql = "INSERT INTO ALBUM_SONG (AlbumID, SongID, TrackNumber) VALUES (?, ?, ?)";
            d1Service.executeUpdateWithParams(linkSql, List.of(existingAlbumId, songId, trackNum));
        }

        return "redirect:/artistdashboard?artistId=" + artistId + "&success=true";
    }

    @GetMapping("/playSong")
    public String playSong(@RequestParam String songId, Model model) {
        String sql = "SELECT s.*, g.GenreName, a.Name as ArtistName " +
                "FROM SONG s " +
                "LEFT JOIN GENRE g ON s.GenreID = g.GenreID " +
                "LEFT JOIN ARTIST a ON s.ArtistID = a.ArtistID " +
                "WHERE s.SongID = ?";

        D1Response response = d1Service.executeQueryWithParams(sql, List.of(songId));
        List<Map<String, Object>> results = d1Service.getResults(response);

        if (!results.isEmpty()) {
            model.addAttribute("song", results.get(0));
            return "playsong";
        }
        return "redirect:/artistlogin";
    }

    @GetMapping("/album/{albumId}")
    public String showAlbumDetails(@PathVariable String albumId, Model model) {
        String albumSql = "SELECT alb.*, art.Name as ArtistName " +
                "FROM ALBUM alb " +
                "JOIN ARTIST art ON alb.ArtistID = art.ArtistID " +
                "WHERE alb.AlbumID = ?";
        D1Response albumRes = d1Service.executeQueryWithParams(albumSql, List.of(albumId));

        String songsSql = "SELECT s.*, asg.TrackNumber " +
                "FROM SONG s " +
                "JOIN ALBUM_SONG asg ON s.SongID = asg.SongID " +
                "WHERE asg.AlbumID = ? " +
                "ORDER BY asg.TrackNumber ASC";
        D1Response songsRes = d1Service.executeQueryWithParams(songsSql, List.of(albumId));

        model.addAttribute("album", d1Service.getResults(albumRes).get(0));
        model.addAttribute("songs", d1Service.getResults(songsRes));
        return "albumdetails";
    }

    @GetMapping("/artist/settings")
    public String showSettings(@RequestParam String artistId, Model model) {
        String sql = "SELECT * FROM ARTIST WHERE ArtistID = ?";
        D1Response response = d1Service.executeQueryWithParams(sql, List.of(artistId));
        model.addAttribute("artist", d1Service.getResults(response).get(0));
        return "artist-settings";
    }

    @PostMapping("/artist/update")
    public String updateArtist(@RequestParam String artistId,
                               @RequestParam String name,
                               @RequestParam String password) {
        String sql = "UPDATE ARTIST SET Name = ?, Password = ? WHERE ArtistID = ?";
        d1Service.executeUpdateWithParams(sql, List.of(name, password, artistId));
        return "redirect:/artistdashboard?artistId=" + artistId + "&updated=true";
    }

    @PostMapping("/artist/delete")
    public String deleteArtist(@RequestParam String artistId) {
        String getSongsSql = "SELECT SongID, AudioFileURL, CoverArtURL FROM SONG WHERE ArtistID = ?";
        D1Response songsRes = d1Service.executeQueryWithParams(getSongsSql, List.of(artistId));
        List<Map<String, Object>> songs = d1Service.getResults(songsRes);

        for (Map<String, Object> song : songs) {
            String songId = (String) song.get("SongID");
            String audioKey = (String) song.get("AudioFileURL");
            String coverKey = (String) song.get("CoverArtURL");

            d1Service.executeUpdateWithParams("DELETE FROM PLAYLIST_SONG WHERE SongID = ?", List.of(songId));
            d1Service.executeUpdateWithParams("DELETE FROM ALBUM_SONG WHERE SongID = ?", List.of(songId));

            musicService.deleteSong(audioKey);
            musicService.deleteSong(coverKey);
        }

        String getAlbumsSql = "SELECT CoverArtURL FROM ALBUM WHERE ArtistID = ?";
        D1Response albumsRes = d1Service.executeQueryWithParams(getAlbumsSql, List.of(artistId));
        List<Map<String, Object>> albums = d1Service.getResults(albumsRes);
        for (Map<String, Object> album : albums) {
            musicService.deleteSong((String) album.get("CoverArtURL"));
        }

        d1Service.executeUpdateWithParams("DELETE FROM ALBUM WHERE ArtistID = ?", List.of(artistId));
        d1Service.executeUpdateWithParams("DELETE FROM SONG WHERE ArtistID = ?", List.of(artistId));
        d1Service.executeUpdateWithParams("DELETE FROM ARTIST WHERE ArtistID = ?", List.of(artistId));

        return "redirect:/artistlogin?deleted=true";
    }
}