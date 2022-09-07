package com.amplitude.api;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(manifest=Config.NONE)
public class IngestionMetadataTest {

    @Test
    public void testToJSONObject() throws JSONException {
        IngestionMetadata ingestionMetadata = new IngestionMetadata();
        String sourceName = "ampli";
        String sourceVersion = "1.0.0";
        ingestionMetadata.setSourceName(sourceName)
                .setSourceVersion(sourceVersion);
        JSONObject result = ingestionMetadata.toJSONObject();
        assertEquals(sourceName, result.getString(Constants.AMP_INGESTION_METADATA_SOURCE_NAME));
        assertEquals(sourceVersion, result.getString(Constants.AMP_INGESTION_METADATA_SOURCE_VERSION));
    }
}
