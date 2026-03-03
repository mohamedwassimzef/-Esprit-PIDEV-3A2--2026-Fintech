package tn.esprit.services;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BoldSignDownloader {

    public static void downloadDocument(String documentId, String savePath, String apiKey) throws Exception {
        String urlStr = "https://api.boldsign.com/v1/documents/" + documentId + "/download";
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Accept", "application/pdf");

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(savePath)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                System.out.println("Document downloaded successfully: " + savePath);
            }
        } else {
            System.out.println("Failed to download document. HTTP code: " + responseCode);
        }

        conn.disconnect();
    }

    public static void main(String[] args) throws Exception {
        String documentId = "19672e4c-d63d-42c2-95b6-3ed10d8d5301";
        String apiKey = "<YOUR_API_KEY>";
        String savePath = "user_contracts/contract.pdf";

        downloadDocument(documentId, savePath, apiKey);
    }
}