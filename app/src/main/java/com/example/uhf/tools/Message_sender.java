package com.example.uhf.tools;

import android.os.AsyncTask;

import com.example.uhf.fragment.UHFReadTagFragment;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class Message_sender {
    public static boolean messageSender(ArrayList<HashMap<String, String>> tagList){
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < tagList.size(); i++) {
            String epcTag = tagList.get(i).get(UHFReadTagFragment.TAG_EPC_TID);
            String rfidNumber = epcTag.substring(4);  // This will remove the first 4 characters

            message.append(rfidNumber);
            if (i != tagList.size() - 1) {
                message.append(",");  // Add a comma if it's not the last element
            }
        }
        new SendMessageTask().execute(message.toString());
        return true;
    }

    private static class SendMessageTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... messages) {
            for (String message : messages) {
                try {
                    sendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    private static void sendMessage(String message) throws IOException {
        String urlString = "https://rfidpartnership.com/api/data/1";
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        String payload = "{\"data\": \"" + message + "\"}";

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = payload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int statusCode = connection.getResponseCode();
        String responseMessage = connection.getResponseMessage();

        if (statusCode != 200) {
            throw new IOException("Failed to send message. Status code: " + statusCode + ", Response: " + responseMessage);
        }
    }
}