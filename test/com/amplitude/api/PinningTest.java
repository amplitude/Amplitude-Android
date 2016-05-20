package com.amplitude.api;

import android.os.SystemClock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PinningTest extends BaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp(false);
        // need to set clock > 0 so that logThread posts in order
        SystemClock.setCurrentTimeMillis(1000);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testSslPinning() {
        amplitude = PinnedAmplitudeClient.getInstance();
        amplitude.initialize(context, "1cc2c1978ebab0f6451112a8f5df4f4e");
        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runOneTask();
        looper.runOneTask();

        amplitude.logEvent("pinned_test_event", null);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        ShadowLooper httplooper = Shadows.shadowOf(amplitude.httpThread.getLooper());
        httplooper.runToEndOfTasks();

        assertNull(amplitude.lastError);
    }

    private static class InvalidPinnedAmplitudeClient extends PinnedAmplitudeClient {
        public static final SSLContextBuilder INVALID_SSL_CONTEXT = new SSLContextBuilder()
          .addCertificate(""
              + "MIIFVjCCBD6gAwIBAgIRAObsedhCFsMHaYL156gA4XAwDQYJKoZIhvcNAQELBQAwgZ"
              + "AxCzAJBgNVBAYTAkdCMRswGQYDVQQIExJHcmVhdGVyIE1hbmNoZXN0ZXIxEDAOBgNV"
              + "BAcTB1NhbGZvcmQxGjAYBgNVBAoTEUNPTU9ETyBDQSBMaW1pdGVkMTYwNAYDVQQDEy"
              + "1DT01PRE8gUlNBIERvbWFpbiBWYWxpZGF0aW9uIFNlY3VyZSBTZXJ2ZXIgQ0EwHhcN"
              + "MTQwODE0MDAwMDAwWhcNMTkwODEzMjM1OTU5WjBcMSEwHwYDVQQLExhEb21haW4gQ2"
              + "9udHJvbCBWYWxpZGF0ZWQxHTAbBgNVBAsTFFBvc2l0aXZlU1NMIFdpbGRjYXJkMRgw"
              + "FgYDVQQDFA8qLnlpa3lha2FwaS5uZXQwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwgg"
              + "EKAoIBAQDANfb+9W5g48LTQezWOMdQlL6kE66mqAnR9GAM1Ron31WuHQFn52Y/A6KK"
              + "EfUUHIcC/3vgLHRGzWlzPs8ctTHIMH++Tb2eS3uNhyeiQ2ZTALYFslNThZsdoh/kYu"
              + "YD5qX55ZKP1DJxm2ftcR38XoyWH1mv/JsT1Hq6/ATsesJJxwzxjI2G4NZyPFm8c2w8"
              + "EhfdBgDGyBliGo24TpM7uOVYmC01mAB/pZZS2EuBWjhA6Ny7pTLnjIBAx3jh8Vd3is"
              + "cMxq5boq2DKD0rSVQWbWdYbFMvvMmvq8qnuX9IijqbykoHoHGKerE/LaDs3xDZTlpE"
              + "AvAt8oFUyAllUNCairq3AgMBAAGjggHcMIIB2DAfBgNVHSMEGDAWgBSQr2o6lFoL2J"
              + "DqElZz30O0Oija5zAdBgNVHQ4EFgQUMpYvhtltmd1BhVilRheMd6wqENYwDgYDVR0P"
              + "AQH/BAQDAgWgMAwGA1UdEwEB/wQCMAAwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQ"
              + "UFBwMCMFAGA1UdIARJMEcwOwYMKwYBBAGyMQECAQMEMCswKQYIKwYBBQUHAgEWHWh0"
              + "dHBzOi8vc2VjdXJlLmNvbW9kby5uZXQvQ1BTMAgGBmeBDAECATBUBgNVHR8ETTBLME"
              + "mgR6BFhkNodHRwOi8vY3JsLmNvbW9kb2NhLmNvbS9DT01PRE9SU0FEb21haW5WYWxp"
              + "ZGF0aW9uU2VjdXJlU2VydmVyQ0EuY3JsMIGFBggrBgEFBQcBAQR5MHcwTwYIKwYBBQ"
              + "UHMAKGQ2h0dHA6Ly9jcnQuY29tb2RvY2EuY29tL0NPTU9ET1JTQURvbWFpblZhbGlk"
              + "YXRpb25TZWN1cmVTZXJ2ZXJDQS5jcnQwJAYIKwYBBQUHMAGGGGh0dHA6Ly9vY3NwLm"
              + "NvbW9kb2NhLmNvbTApBgNVHREEIjAggg8qLnlpa3lha2FwaS5uZXSCDXlpa3lha2Fw"
              + "aS5uZXQwDQYJKoZIhvcNAQELBQADggEBAEdw5iJwxvXcZlPQEbudu84VI48uwSYcGz"
              + "xBVzOsfYPdLUc7HSYT8zwg2d5l89iLiapvHS6gQiASZRi7nzR/oqnARcVjWnvIKPlq"
              + "+b3OUtNElnfSFXZnsgpxp3BlcCEfQs7faII89rTzxJqRf0fNo8Y4u3+k79zGF8xbon"
              + "O8oXZt0ApxcYmFIQlhCddM20lgHLTeMx4yG5C2lHGJE3iUS7YVAq6ENRrgiVhcuf5R"
              + "H1mWAYpFPJ7rOmpCReC6brxCho/7jg+fBqEUfCGyrMtYSRejCc9aZGBQmuz5v5iT6P"
              + "XCBeVmjEX3kh4bkRPHJ5vyASNXUkF3nwVAe4cwOoLHN8o=");

        public InvalidPinnedAmplitudeClient() {
            super();
            super.getPinnedCertSslSocketFactory(INVALID_SSL_CONTEXT);
        }
    }

    @Test
    public void testSslPinningInvalid() {
        amplitude = new InvalidPinnedAmplitudeClient();
        amplitude.initialize(context, "1cc2c1978ebab0f6451112a8f5df4f4e");

        ShadowLooper looper = Shadows.shadowOf(amplitude.logThread.getLooper());
        looper.runToEndOfTasks();

        amplitude.logEvent("pinned_test_event_invalid", null);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        ShadowLooper httplooper = Shadows.shadowOf(amplitude.httpThread.getLooper());
        httplooper.runToEndOfTasks();

        assertNotNull(amplitude.lastError);
    }
}
