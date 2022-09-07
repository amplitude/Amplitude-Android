package com.amplitude.api;

import org.json.JSONException;
import org.json.JSONObject;

public class IngestionMetadata {
    private static final String TAG = IngestionMetadata.class.getName();
    /**
     * The source name, e.g. "ampli"
     */
    private String sourceName;
    /**
     * The source version, e.g. "2.0.0"
     */
    private String sourceVersion;

    /**
     * Set the ingestion metadata source name information.
     * @param sourceName source name for ingestion metadata
     * @return the same IngestionMetadata object
     */
    public IngestionMetadata setSourceName(String sourceName) {
        this.sourceName = sourceName;
        return this;
    }

    /**
     * Set the ingestion metadata source version information.
     * @param sourceVersion source version for ingestion metadata
     * @return the same IngestionMetadata object
     */
    public IngestionMetadata setSourceVersion(String sourceVersion) {
        this.sourceVersion = sourceVersion;
        return this;
    }

    /**
     * Get JSONObject of current ingestion metadata
     * @return JSONObject including ingestion metadata information
     */
    protected JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            if (!Utils.isEmptyString(sourceName)) {
                jsonObject.put(Constants.AMP_INGESTION_METADATA_SOURCE_NAME, sourceName);
            }
            if (!Utils.isEmptyString(sourceVersion)) {
                jsonObject.put(Constants.AMP_INGESTION_METADATA_SOURCE_VERSION, sourceVersion);
            }
        } catch (JSONException e) {
            AmplitudeLog.getLogger().e(TAG, "JSON Serialization of ingestion metadata object failed");
        }
        return jsonObject;
    }
}
