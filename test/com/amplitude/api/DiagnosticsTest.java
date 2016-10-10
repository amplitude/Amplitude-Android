package com.amplitude.api;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DiagnosticsTest extends BaseTest {

    private Diagnostics logger;

    @Before
    public void setUp() throws Exception {
        super.setUp(false);
        logger = Diagnostics.getLogger().setEnabled(true);
        logger.setMaxKeys(Diagnostics.MAX_UNSENT_DIAGNOSTIC_QUEUE_SIZE);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        logger.clear();
        logger = null;
    }

    @Test
    public void testInitializeInstance() {
        assertEquals(logger.getUnsentErrorsJSON().length(), 0);
    }

    @Test
    public void testLogErrors(){
        logger.setMaxKeys(3);

        String error1 = "error1";
        String error2 = "error2";
        String error3 = "error3";
        String error4 = "error4";

        logger.logError(error1);
        logger.logError(error1);
        logger.logError(error2);
        logger.logError(error1);
        logger.logError(error2);
        logger.logError(error3);
        logger.logError(error4);
        logger.logError(error4);

        JSONObject unsentErrors = logger.getUnsentErrorsJSON();
        assertEquals(unsentErrors.length(), 3);
        assertTrue(unsentErrors.has(error1));
        assertEquals(unsentErrors.optInt(error1), 3);
        assertTrue(unsentErrors.has(error2));
        assertEquals(unsentErrors.optInt(error2), 2);
        assertTrue(unsentErrors.has(error3));
        assertEquals(unsentErrors.optInt(error3), 1);
        assertFalse(unsentErrors.has(error4));
    }

    @Test
    public void testClear() {
        String error1 = "error1";
        String error2 = "error2";

        logger.logError(error1);
        logger.logError(error1);
        logger.logError(error2);
        logger.logError(error1);
        logger.logError(error2);

        JSONObject unsentErrors = logger.getUnsentErrorsJSON();
        assertEquals(unsentErrors.length(), 2);
        assertTrue(unsentErrors.has(error1));
        assertEquals(unsentErrors.optInt(error1), 3);
        assertTrue(unsentErrors.has(error2));
        assertEquals(unsentErrors.optInt(error2), 2);

        logger.clear();
        unsentErrors = logger.getUnsentErrorsJSON();
        assertEquals(unsentErrors.length(), 0);
        assertFalse(unsentErrors.has(error1));
        assertFalse(unsentErrors.has(error2));
    }
}

