package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mpatric.mp3agic.*;
import io.restassured.path.json.JsonPath;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

public class Tagger {

    // TODO: retrieve cover art from youtube music instead
    static int MODE = 2;
    
    public static void main(String[] args) {
        new guiTagger();
    }

    public static void tagAllFiles() throws InvalidDataException, UnsupportedTagException, IOException, URISyntaxException, InterruptedException, NotSupportedException {
        File file = new File("C:\\Users\\harry\\Downloads\\");
        File[] songs = file.listFiles(filter);
        if (songs != null) {
            for (File mp3 : songs) {
                tagFile(mp3.getAbsolutePath(), false, null);
            }
        } else {
            System.out.println("There are no songs in your downloads folder!");
        }
    }

    public static void tagFile(String filePath, boolean individual, String vID) throws InvalidDataException, UnsupportedTagException, IOException, URISyntaxException, InterruptedException, NotSupportedException {
        ID3v2 id3v2Tag = null;
        String prefixToRemove = "C:\\Users\\harry\\Downloads\\";
        Mp3File mp3file = new Mp3File(filePath);
        String songName = mp3file.getFilename().substring(prefixToRemove.length(), mp3file.getFilename().length() - 4);
        System.out.println(songName);
        String[] splitSong = songName.split(" - ");
        System.out.println(Arrays.toString(splitSong));
        id3v2Tag = addArtistAndSongname(splitSong, mp3file);
        if (id3v2Tag == null) {
            if (mp3file.hasId3v2Tag()) {
                id3v2Tag = mp3file.getId3v2Tag();
            } else {
                // mp3 does not have an ID3v2 tag, let's create one
                id3v2Tag = new ID3v24Tag();
                mp3file.setId3v1Tag(id3v2Tag);
            }
        }
        File img;
        if (MODE == 0) {
            String mbid = getMbid(splitSong[1], splitSong[0]);
            img = getCoverArt(mbid);
        } else if (MODE == 1) {
            if (!individual) {
                img = getCoverArtNew(songName, false);
            } else {
                img = getCoverArtNew(vID, true);
            }
        } else if (MODE == 2) {
            if (!individual) {
                img = getCoverArtNewest(songName, false);
            } else {
                img = getCoverArtNewest(vID, true);
            }
        } else {
            System.out.println("Unidentified mode variable used!");
            return;
        }
        byte[] bytes = FileUtils.readFileToByteArray(img);
        String mimeType = Files.probeContentType(img.toPath());
        id3v2Tag.setAlbumImage(bytes, mimeType);

        File tempMp3File = File.createTempFile("temp", ".mp3");
        mp3file.save(tempMp3File.getAbsolutePath()); // Save to temporary file

        // Replace original file with the temporary file
        Files.move(tempMp3File.toPath(), new File(filePath).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Tagging complete.");
    }

    private static ID3v2 addArtistAndSongname(String[] splitSong, Mp3File mp3file) {
        ID3v2 id3v2Tag = null;
        if (splitSong.length == 2) {
            if (mp3file.hasId3v2Tag()) {
                id3v2Tag = mp3file.getId3v2Tag();
            } else {
                // mp3 does not have an ID3v2 tag, let's create one
                id3v2Tag = new ID3v24Tag();
                mp3file.setId3v1Tag(id3v2Tag);
            }
            id3v2Tag.setArtist(splitSong[0]);
            id3v2Tag.setTitle(splitSong[1]);
        }
        return id3v2Tag;
    }

    public static String getMbid(String songName, String artistName) throws URISyntaxException, IOException, InterruptedException {
        // Base URL of the MusicBrainz API
        String baseUrl = "https://musicbrainz.org/ws/2/";

        // Endpoint path for search
        String endpoint = "recording";

        HttpClient client = HttpClient.newHttpClient();

        // Encode the song name for URL
        String encodedSongName = URLEncoder.encode(songName, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder(new URI(baseUrl + endpoint + "?query=recording:" + encodedSongName + "%20AND%20artistname:" + artistName + "%20AND%20status:official%20AND%20primarytype:album&inc=releases&limit=3&fmt=json"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        // Parse JSON response
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonResponse = response.body();
        Object json = gson.fromJson(jsonResponse, Object.class); // Deserialize JSON to Object

        // Print formatted JSON
        String formattedJson = gson.toJson(json);
        System.out.println("Formatted response:\n" + formattedJson);

        if (LevenshteinDistance.getDefaultInstance().apply(songName, JsonPath.from(formattedJson).get("recordings[0].title")) > 7) {
            System.out.println("Song names not similar enough!");
            return null;
        }

        String mbid = JsonPath.from(formattedJson).get("recordings[0].releases[0].id");
        System.out.println(mbid);
        return mbid;
    }

    public static File getCoverArt(String mbid) throws URISyntaxException, IOException, InterruptedException {
        String baseUrl = "http://coverartarchive.org";
        String endpoint = "/release/" + mbid + "/front";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(new URI(baseUrl + endpoint))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String jsonResponse = response.body();
        System.out.println(jsonResponse.substring(5));
        URL url = new URL(jsonResponse.substring(5));
        File img = new File("img.jpg");
        FileUtils.copyURLToFile(url, img);
        return img;
    }

    public static File getCoverArtNew(String songName, boolean individual) throws IOException, URISyntaxException, InterruptedException {
        String vID;
        if (!individual) {
            String encodedSongName = URLEncoder.encode(songName, StandardCharsets.UTF_8);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(new URI("https://www.googleapis.com/youtube/v3/search?key=***REMOVED***&part=snippet&maxResults=2&topicId=/m/04rlf&q=" + encodedSongName))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            // Parse JSON response
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonResponse = response.body();
            Object json = gson.fromJson(jsonResponse, Object.class); // Deserialize JSON to Object

            // Print formatted JSON
            String formattedJson = gson.toJson(json);
            System.out.println("Formatted response:\n" + formattedJson);
            vID = JsonPath.from(formattedJson).get("items[0].id.videoId");
        } else {
            vID = songName;
        }
        System.out.println(vID);
        return getCroppedImageFromVID(vID);
    }

    public static File getCoverArtNewest(String songName, boolean individual) throws IOException, InterruptedException {
        String filePath = "C:\\Users\\harry\\Downloads\\Tools\\Auto-tagger\\coverArt.py";
        ProcessBuilder pb = new ProcessBuilder()
                .command("python", "-u", filePath, "main33", songName);
        Process p = pb.start();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
        StringBuilder buffer = new StringBuilder();
        String line = null;
        while ((line = in.readLine()) != null){
            buffer.append(line);
        }
        int exitCode = p.waitFor();
        System.out.println("Value is: "+buffer.toString());
        String vID = buffer.toString();
        in.close();
        return getCroppedImageFromVID(vID);
    }

    private static @NotNull File getCroppedImageFromVID(String vID) throws IOException {
        URL url = new URL("https://i.ytimg.com/vi/" + vID + "/maxresdefault.jpg");
        File img = new File("img.jpg");
        try {
            FileUtils.copyURLToFile(url, img);
        } catch (FileNotFoundException e) {
            try {
                url = new URL("https://i.ytimg.com/vi/" + vID + "/hq720.jpg");
                FileUtils.copyURLToFile(url, img);
            } catch (FileNotFoundException ex) {
                url = new URL("https://i.ytimg.com/vi/" + vID + "/hqdefault.jpg");
                FileUtils.copyURLToFile(url, img);
            }
        }
        BufferedImage bufferedImg = ImageIO.read(img);
        int targetWidth = bufferedImg.getHeight();
        int startX = (bufferedImg.getWidth() / 2) - (targetWidth / 2);
        BufferedImage croppedImage = bufferedImg.getSubimage(startX, 0, targetWidth, targetWidth);
        File outputFile = new File("croppedImage.jpg");
        ImageIO.write(croppedImage, "jpg", outputFile);
        return outputFile;
    }

    // file filter for sort mp3 files
    static FileFilter filter = file -> file.getName().endsWith(".mp3");
}