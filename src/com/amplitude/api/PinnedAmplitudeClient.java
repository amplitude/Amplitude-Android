package com.amplitude.api;

import android.content.Context;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import okhttp3.OkHttpClient;
import okio.Buffer;
import okio.ByteString;

/**
 * <h1>PinnedAmplitudeClient</h1>
 * This is a version of the AmplitudeClient that supports SSL pinning for encrypted requests.
 * Please contact <a href="mailto:support@amplitude.com">Amplitude Support</a> before you ship any
 * products with SSL pinning enabled so that we are aware and can provide documentation
 * and implementation help.
 */
public class PinnedAmplitudeClient extends AmplitudeClient {

    /**
     * The class identifier tag used in logging. TAG = {@code "com.amplitude.api.PinnedAmplitudeClient";}
     */
    public static final String TAG = "com.amplitude.api.PinnedAmplitudeClient";
    private static final AmplitudeLog logger = AmplitudeLog.getLogger();


    /**
     * Pinned certificate chain for api.amplitude.com.
     */
    protected static final SSLContextBuilder SSL_CONTEXT_API_AMPLITUDE_COM = new SSLContextBuilder()
        // CN=COMODO RSA Domain Validation Secure Server CA, O=COMODO CA Limited,
        // L=Salford, ST=Greater Manchester, C=GB
        .addCertificate(""
            + "MIIGCDCCA/CgAwIBAgIQKy5u6tl1NmwUim7bo3yMBzANBgkqhkiG9w0BAQwFADCBhT"
            + "ELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4GA1UE"
            + "BxMHU2FsZm9yZDEaMBgGA1UEChMRQ09NT0RPIENBIExpbWl0ZWQxKzApBgNVBAMTIk"
            + "NPTU9ETyBSU0EgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkwHhcNMTQwMjEyMDAwMDAw"
            + "WhcNMjkwMjExMjM1OTU5WjCBkDELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZX"
            + "IgTWFuY2hlc3RlcjEQMA4GA1UEBxMHU2FsZm9yZDEaMBgGA1UEChMRQ09NT0RPIENB"
            + "IExpbWl0ZWQxNjA0BgNVBAMTLUNPTU9ETyBSU0EgRG9tYWluIFZhbGlkYXRpb24gU2"
            + "VjdXJlIFNlcnZlciBDQTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAI7C"
            + "AhnhoFmk6zg1jSz9AdDTScBkxwtiBUUWOqigwAwCfx3M28ShbXcDow+G+eMGnD4LgY"
            + "qbSRutA776S9uMIO3Vzl5ljj4Nr0zCsLdFXlIvNN5IJGS0Qa4Al/e+Z96e0HqnU4A7"
            + "fK31llVvl0cKfIWLIpeNs4TgllfQcBhglo/uLQeTnaG6ytHNe+nEKpooIZFNb5JPJa"
            + "XyejXdJtxGpdCsWTWM/06RQ1A/WZMebFEh7lgUq/51UHg+TLAchhP6a5i84DuUHoVS"
            + "3AOTJBhuyydRReZw3iVDpA3hSqXttn7IzW3uLh0nc13cRTCAquOyQQuvvUSH2rnlG5"
            + "1/ruWFgqUCAwEAAaOCAWUwggFhMB8GA1UdIwQYMBaAFLuvfgI9+qbxPISOre44mOzZ"
            + "MjLUMB0GA1UdDgQWBBSQr2o6lFoL2JDqElZz30O0Oija5zAOBgNVHQ8BAf8EBAMCAY"
            + "YwEgYDVR0TAQH/BAgwBgEB/wIBADAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUH"
            + "AwIwGwYDVR0gBBQwEjAGBgRVHSAAMAgGBmeBDAECATBMBgNVHR8ERTBDMEGgP6A9hj"
            + "todHRwOi8vY3JsLmNvbW9kb2NhLmNvbS9DT01PRE9SU0FDZXJ0aWZpY2F0aW9uQXV0"
            + "aG9yaXR5LmNybDBxBggrBgEFBQcBAQRlMGMwOwYIKwYBBQUHMAKGL2h0dHA6Ly9jcn"
            + "QuY29tb2RvY2EuY29tL0NPTU9ET1JTQUFkZFRydXN0Q0EuY3J0MCQGCCsGAQUFBzAB"
            + "hhhodHRwOi8vb2NzcC5jb21vZG9jYS5jb20wDQYJKoZIhvcNAQEMBQADggIBAE4rdk"
            + "+SHGI2ibp3wScF9BzWRJ2pmj6q1WZmAT7qSeaiNbz69t2Vjpk1mA42GHWx3d1Qcnyu"
            + "3HeIzg/3kCDKo2cuH1Z/e+FE6kKVxF0NAVBGFfKBiVlsit2M8RKhjTpCipj4SzR7Jz"
            + "sItG8kO3KdY3RYPBpsP0/HEZrIqPW1N+8QRcZs2eBelSaz662jue5/DJpmNXMyYE7l"
            + "3YphLG5SEXdoltMYdVEVABt0iN3hxzgEQyjpFv3ZBdRdRydg1vs4O2xyopT4Qhrf7W"
            + "8GjEXCBgCq5Ojc2bXhc3js9iPc0d1sjhqPpepUfJa3w/5Vjo1JXvxku88+vZbrac2/"
            + "4EjxYoIQ5QxGV/Iz2tDIY+3GH5QFlkoakdH368+PUq4NCNk+qKBR6cGHdNXJ93SrLl"
            + "P7u3r7l+L4HyaPs9Kg4DdbKDsx5Q5XLVq4rXmsXiBmGqW5prU5wfWYQ//u+aen/e7K"
            + "JD2AFsQXj4rBYKEMrltDR5FL1ZoXX/nUh8HCjLfn4g8wGTeGrODcQgPmlKidrv0PJF"
            + "GUzpII0fxQ8ANAe4hZ7Q7drNJ3gjTcBpUC2JD5Leo31Rpg0Gcg19hCC0Wvgmje3WYk"
            + "N5AplBlGGSW4gNfL1IYoakRwJiNiqZ+Gb7+6kHDSVneFeO/qJakXzlByjAA6quPbYz"
            + "Sf+AZxAeKCINT+b72x")
        // CN=COMODO RSA Certification Authority, O=COMODO CA Limited, L=Salford,
        // ST=Greater Manchester, C=GB
        .addCertificate(""
            + "MIIFdDCCBFygAwIBAgIQJ2buVutJ846r13Ci/ITeIjANBgkqhkiG9w0BAQwFADBvMQ"
            + "swCQYDVQQGEwJTRTEUMBIGA1UEChMLQWRkVHJ1c3QgQUIxJjAkBgNVBAsTHUFkZFRy"
            + "dXN0IEV4dGVybmFsIFRUUCBOZXR3b3JrMSIwIAYDVQQDExlBZGRUcnVzdCBFeHRlcm"
            + "5hbCBDQSBSb290MB4XDTAwMDUzMDEwNDgzOFoXDTIwMDUzMDEwNDgzOFowgYUxCzAJ"
            + "BgNVBAYTAkdCMRswGQYDVQQIExJHcmVhdGVyIE1hbmNoZXN0ZXIxEDAOBgNVBAcTB1"
            + "NhbGZvcmQxGjAYBgNVBAoTEUNPTU9ETyBDQSBMaW1pdGVkMSswKQYDVQQDEyJDT01P"
            + "RE8gUlNBIENlcnRpZmljYXRpb24gQXV0aG9yaXR5MIICIjANBgkqhkiG9w0BAQEFAA"
            + "OCAg8AMIICCgKCAgEAkehUktIKVrGsDSTdxc9EZ3SZKzejfSNwAHG8U9/E+ioSj0t/"
            + "EFa9n3Byt2F/yUsPF6c947AEYe7/EZfH9IY+Cvo+XPmT5jR62RRr55yzhaCCenavcZ"
            + "DX7P0N+pxs+t+wgvQUfvm+xKYvT3+Zf7X8Z0NyvQwA1onrayzT7Y+YHBSrfuXjbvzY"
            + "qOSSJNpDa2K4Vf3qwbxstovzDo2a5JtsaZn4eEgwRdWt4Q08RWD8MpZRJ7xnw8outm"
            + "vqRsfHIKCxH2XeSAi6pE6p8oNGN4Tr6MyBSENnTnIqm1y9TBsoilwie7SrmNnu4FGD"
            + "wwlGTm0+mfqVF9p8M1dBPI1R7Qu2XK8sYxrfV8g/vOldxJuvRZnio1oktLqpVj3Pb6"
            + "r/SVi+8Kj/9Lit6Tf7urj0Czr56ENCHonYhMsT8dm74YlguIwoVqwUHZwK53Hrzw7d"
            + "PamWoUi9PPevtQ0iTMARgexWO/bTouJbt7IEIlKVgJNp6I5MZfGRAy1wdALqi2cVKW"
            + "lSArvX31BqVUa/oKMoYX9w0MOiqiwhqkfOKJwGRXa/ghgntNWutMtQ5mv0TIZxMOmm"
            + "3xaG4Nj/QN370EKIf6MzOi5cHkERgWPOGHFrK+ymircxXDpqR+DDeVnWIBqv8mqYqn"
            + "K8V0rSS527EPywTEHl7R09XiidnMy/s1Hap0flhFMCAwEAAaOB9DCB8TAfBgNVHSME"
            + "GDAWgBStvZh6NLQm9/rEJlTvA73gJMtUGjAdBgNVHQ4EFgQUu69+Aj36pvE8hI6t7j"
            + "iY7NkyMtQwDgYDVR0PAQH/BAQDAgGGMA8GA1UdEwEB/wQFMAMBAf8wEQYDVR0gBAow"
            + "CDAGBgRVHSAAMEQGA1UdHwQ9MDswOaA3oDWGM2h0dHA6Ly9jcmwudXNlcnRydXN0Lm"
            + "NvbS9BZGRUcnVzdEV4dGVybmFsQ0FSb290LmNybDA1BggrBgEFBQcBAQQpMCcwJQYI"
            + "KwYBBQUHMAGGGWh0dHA6Ly9vY3NwLnVzZXJ0cnVzdC5jb20wDQYJKoZIhvcNAQEMBQ"
            + "ADggEBAGS/g/FfmoXQzbihKVcN6Fr30ek+8nYEbvFScLsePP9NDXRqzIGCJdPDoCpd"
            + "TPW6i6FtxFQJdcfjJw5dhHk3QBN39bSsHNA7qxcS1u80GH4r6XnTq1dFDK8o+tDb5V"
            + "CViLvfhVdpfZLYUspzgb8c8+a4bmYRBbMelC1/kZWSWfFMzqORcUx8Rww7Cxn2obFs"
            + "hj5cqsQugsv5B5a6SE2Q8pTIqXOi6wZ7I53eovNNVZ96YUWYGGjHXkBrI/V5eu+MtW"
            + "uLt29G9HvxPUsE2JOAWVrgQSQdso8VYFhH2+9uRv0V9dlfmrPb2LjkQLPNlzmuhbsd"
            + "jrzch5vRpu/xO28QOG8=");

    /**
     * SSl context builder, used to generate the SSL context.
     */
    protected static class SSLContextBuilder {
        private final List<String> certificateBase64s = new ArrayList<String>();

        /**
         * Add certificate ssl context builder.
         *
         * @param certificateBase64 the certificate base 64
         * @return the ssl context builder
         */
        public SSLContextBuilder addCertificate(String certificateBase64) {
            certificateBase64s.add(certificateBase64);
            return this;
        }

        /**
         * Build ssl context.
         *
         * @return the ssl context
         */
        public SSLContext build() {
            try {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, null); // Use a null input stream + password to create an empty key store.

                // Decode the certificates and add 'em to the key store.
                int nextName = 1;
                for (String certificateBase64 : certificateBase64s) {
                    Buffer certificateBuffer = new Buffer().write(ByteString.decodeBase64(certificateBase64));
                    X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(
                            certificateBuffer.inputStream());
                    keyStore.setCertificateEntry(Integer.toString(nextName++), certificate);
                }

                // Create an SSL context that uses these certificates as its trust store.
                trustManagerFactory.init(keyStore);
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
                return sslContext;
            } catch (GeneralSecurityException e) {
                logger.e(TAG, e.getMessage(), e);
            } catch (IOException e) {
                logger.e(TAG, e.getMessage(), e);
            }
            return null;
        }
    }

    /**
     * The default instance.
     */
    protected static PinnedAmplitudeClient instance = new PinnedAmplitudeClient();

    /**
     * Gets the default instance. Call SDK method on the default instance.
     *
     * @return the default instance
     */
    public static PinnedAmplitudeClient getInstance() {
        return instance;
    }

    /**
     * The SSl socket factory.
     */
    protected SSLSocketFactory sslSocketFactory;

    /**
     * Instantiates a new Pinned amplitude client.
     */
    public PinnedAmplitudeClient() {
        super();
    }

    /**
     * The Initialized ssl socket factory.
     */
    protected boolean initializedSSLSocketFactory = false;

    @Override
    public synchronized AmplitudeClient initialize(Context context, String apiKey, String userId){
        super.initialize(context, apiKey, userId);
        final AmplitudeClient client = this;
        runOnLogThread(new Runnable() {
            @Override
            public void run() {
                if (!initializedSSLSocketFactory) {
                    SSLSocketFactory factory = getPinnedCertSslSocketFactory();
                    if (factory != null) {
                        client.httpClient = new OkHttpClient.Builder().sslSocketFactory(factory).build();
                    } else {
                        logger.e(TAG, "Unable to pin SSL as requested. Will send data without SSL pinning.");
                    }
                    initializedSSLSocketFactory = true;
                }
            }
        });
        return this;
    }

    /**
     * Gets pinned cert ssl socket factory.
     *
     * @return the pinned cert ssl socket factory
     */
    protected SSLSocketFactory getPinnedCertSslSocketFactory() {
        return getPinnedCertSslSocketFactory(SSL_CONTEXT_API_AMPLITUDE_COM);
    }

    /**
     * Gets pinned cert ssl socket factory.
     *
     * @param context the context
     * @return the pinned cert ssl socket factory
     */
    protected SSLSocketFactory getPinnedCertSslSocketFactory(SSLContextBuilder context) {
        if (context == null) {
            return null;
        }
        if (sslSocketFactory == null) {
            try {
                sslSocketFactory = context.build().getSocketFactory();
                logger.i(TAG, "Pinning SSL session using Comodo CA Cert");
            } catch (Exception e) {
                logger.e(TAG, e.getMessage(), e);
            }
        }
        return sslSocketFactory;
    }
}
