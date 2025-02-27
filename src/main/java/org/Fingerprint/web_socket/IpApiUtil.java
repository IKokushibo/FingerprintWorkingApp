package org.Fingerprint.web_socket;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Enumeration;

public class IpApiUtil {
//    public static String getMyIp(){
//        String urlString = "https://api.ipify.org?format=json"; // Example URL
//        StringBuilder response = new StringBuilder();
//
//        try {
//            URL url = new URL(urlString);
//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            connection.setRequestMethod("GET");
//            connection.setConnectTimeout(5000);
//            connection.setReadTimeout(5000);
//
//            // Check the response code
//            int status = connection.getResponseCode();
//            if (status == 200) {
//                // Read the response
//                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    response.append(line);
//                }
//                reader.close();
//            } else {
//                response.append("Error: ").append(status);
//            }
//
//            connection.disconnect();
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//
//        String jsonResponse = response.toString();
//        return jsonResponse.replaceAll(".*\"ip\":\"([^\"]+)\".*", "$1");
//    }

    public static String getMyIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "Unable to get IP";
    }

    public static String authenticate(String username, String password) throws IOException, InterruptedException {
        try {
            // JSON request body for authentication
            String jsonBody = String.format("""
                    {
                        "username": "%s",
                        "password": "%s"
                    }
                    """, username, password);

            // Create HttpClient
            HttpClient client = HttpClient.newHttpClient();

            // Build the HTTP POST request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://dtec-spring-boot-p3ck.onrender.com/api/v1/auth/fingerprint/authenticate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // Send the request and get the response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Print status code
            System.out.println("Status code: " + response.statusCode());

            // Parse the JSON response
            JSONObject jsonResponse = new JSONObject(response.body());
            System.out.println("Response: " + jsonResponse);

            // Extract the Bearer token from the "data" field
            if (jsonResponse.getBoolean("success")) {
                String bearerToken = jsonResponse.getString("data");
                System.out.println("Bearer Token: " + bearerToken);
                return bearerToken;
            }
        } catch (Exception e) {
        }
        return null;
    }

}
