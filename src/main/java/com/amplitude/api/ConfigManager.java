package com.amplitude.api;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class ConfigManager {
    private static String KEY_INGESTION_ENDPOINT = "ingestionEndpoint";

    private static ConfigManager instance = null;

    private String ingestionEndpoint = Constants.EVENT_LOG_URL;

    public String getIngestionEndpoint() {
        return ingestionEndpoint;
    }

    private ConfigManager() {
    }

    public void refresh(RefreshListener listener) {
        try {
            URL obj = new URL(Constants.DYNAMIC_CONFIG_URL);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            int responseCode = con.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject json = new JSONObject(response.toString());
                if (json.has(KEY_INGESTION_ENDPOINT)) {
                    this.ingestionEndpoint = "https://" + json.getString(KEY_INGESTION_ENDPOINT);
                }
            }
        } catch (MalformedURLException e) {

        } catch (IOException e) {

        } catch (JSONException e) {

        } catch (Exception e) {
            
        }

        listener.onFinished();
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }

        return instance;
    }

    interface RefreshListener {
        void onFinished();
    }
}
