package com.amplitude.api;

import android.content.Context;

import com.amplitude.util.DoubleCheck;
import com.amplitude.util.Provider;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

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
    private static final String TAG = PinnedAmplitudeClient.class.getName();

    // CN=COMODO RSA Domain Validation Secure Server CA, O=COMODO CA Limited,
    // L=Salford, ST=Greater Manchester, C=GB
    private static final String CERTIFICATE_US = ""
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
            + "Sf+AZxAeKCINT+b72x";

    private static final String CERTIFICATE_EU = ""
            + "MIIDQTCCAimgAwIBAgITBmyfz5m/jAo54vB4ikPmljZbyjANBgkqhkiG9w0BAQsF\n"
            + "ADA5MQswCQYDVQQGEwJVUzEPMA0GA1UEChMGQW1hem9uMRkwFwYDVQQDExBBbWF6\n"
            + "b24gUm9vdCBDQSAxMB4XDTE1MDUyNjAwMDAwMFoXDTM4MDExNzAwMDAwMFowOTEL\n"
            + "MAkGA1UEBhMCVVMxDzANBgNVBAoTBkFtYXpvbjEZMBcGA1UEAxMQQW1hem9uIFJv\n"
            + "b3QgQ0EgMTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALJ4gHHKeNXj\n"
            + "ca9HgFB0fW7Y14h29Jlo91ghYPl0hAEvrAIthtOgQ3pOsqTQNroBvo3bSMgHFzZM\n"
            + "9O6II8c+6zf1tRn4SWiw3te5djgdYZ6k/oI2peVKVuRF4fn9tBb6dNqcmzU5L/qw\n"
            + "IFAGbHrQgLKm+a/sRxmPUDgH3KKHOVj4utWp+UhnMJbulHheb4mjUcAwhmahRWa6\n"
            + "VOujw5H5SNz/0egwLX0tdHA114gk957EWW67c4cX8jJGKLhD+rcdqsq08p8kDi1L\n"
            + "93FcXmn/6pUCyziKrlA4b9v7LWIbxcceVOF34GfID5yHI9Y/QCB/IIDEgEw+OyQm\n"
            + "jgSubJrIqg0CAwEAAaNCMEAwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMC\n"
            + "AYYwHQYDVR0OBBYEFIQYzIU07LwMlJQuCFmcx7IQTgoIMA0GCSqGSIb3DQEBCwUA\n"
            + "A4IBAQCY8jdaQZChGsV2USggNiMOruYou6r4lK5IpDB/G/wkjUu0yKGX9rbxenDI\n"
            + "U5PMCCjjmCXPI6T53iHTfIUJrU6adTrCC2qJeHZERxhlbI1Bjjt/msv0tadQ1wUs\n"
            + "N+gDS63pYaACbvXy8MWy7Vu33PqUXHeeE6V/Uq2V8viTO96LXFvKWlJbYK8U90vv\n"
            + "o/ufQJVtMVT8QtPHRh8jrdkPSHCa2XV4cdFyQzR1bldZwgJcJmApzyMZFo6IQ6XU\n"
            + "5MsI+yMRQ+hDKXJioaldXgjUkK642M4UwtBV8ob2xJNDd2ZhwLnoQdeXeGADbkpy\n"
            + "rqXRfboQnoZsG4q5WTP468SQvvG5";

    private static final AmplitudeLog logger = AmplitudeLog.getLogger();

    protected static String getCertificate(AmplitudeServerZone serverZone) {
        return (serverZone == AmplitudeServerZone.EU) ? CERTIFICATE_EU : CERTIFICATE_US;
    }

    /**
     * Pinned certificate chain for api.amplitude.com.
     */
    protected static SSLContextBuilder getPinnedCertificateChain(AmplitudeServerZone serverZone) {
        String CERTIFICATE = getCertificate(serverZone);
        return new SSLContextBuilder(serverZone).addCertificate(CERTIFICATE);
    }

    /**
     * SSl context builder, used to generate the SSL context.
     */
    protected static class SSLContextBuilder {
        private final List<String> certificateBase64s = new ArrayList<String>();
        protected AmplitudeServerZone serverZone;

        public SSLContextBuilder() {
            this.serverZone = AmplitudeServerZone.US;
        }
        public SSLContextBuilder(AmplitudeServerZone serverZone) {
            this.serverZone = serverZone;
        }

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

    static Map<String, PinnedAmplitudeClient> instances = new HashMap<String, PinnedAmplitudeClient>();

    /**
     * Gets the default instance.
     *
     * @return the default instance
     */
    public static PinnedAmplitudeClient getInstance() {
        return getInstance(null);
    }

    /**
     * Gets the specified instance. If instance is null or empty string, fetches the default
     * instance instead.
     *
     * @param instance name to get "ex app 1"
     * @return the specified instance
     */
    public static synchronized PinnedAmplitudeClient getInstance(String instance) {
        instance = Utils.normalizeInstanceName(instance);
        PinnedAmplitudeClient client = instances.get(instance);
        if (client == null) {
            client = new PinnedAmplitudeClient(instance);
            instances.put(instance, client);
        }
        return client;
    }

    /**
     * The SSl socket factory.
     */
    protected SSLSocketFactory sslSocketFactory;

    /**
     * Instantiates a new Pinned amplitude client.
     */
    public PinnedAmplitudeClient(String instance) {
        super(instance);
    }

    /**
     * The Initialized ssl socket factory.
     */
    protected boolean initializedSSLSocketFactory = false;

    public synchronized AmplitudeClient initializeInternal(
            Context context,
            String apiKey,
            String userId,
            Provider<OkHttpClient> clientProvider
    ) {
        super.initialize(context, apiKey, userId);
        final PinnedAmplitudeClient client = this;
        runOnLogThread(new Runnable() {
            @Override
            public void run() {
                if (!client.initializedSSLSocketFactory) {
                    SSLSocketFactory factory = getPinnedCertSslSocketFactory(client.getServerZone());
                    if (factory != null) {
                        try {
                            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                                    TrustManagerFactory.getDefaultAlgorithm());
                            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                            keyStore.load(null, null); // Use a null input stream + password to create an empty key store.

                            List<String> certificateBase64s = new ArrayList<String>();
                            String CERTIFICATE = getCertificate(client.getServerZone());
                            certificateBase64s.add(CERTIFICATE);

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

                            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                                throw new IllegalStateException("Unexpected default trust managers:"
                                        + Arrays.toString(trustManagers));
                            }
                            X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
                            final Provider<OkHttpClient> finalClientProvider = DoubleCheck.provider(() -> {
                                final OkHttpClient.Builder builder;
                                if (clientProvider != null) {
                                    builder = clientProvider.get().newBuilder();
                                } else {
                                    builder = new OkHttpClient.Builder();
                                }
                                return builder.sslSocketFactory(factory, trustManager).build();
                            });

                            client.callFactory = request -> finalClientProvider.get().newCall(request);
                        } catch (GeneralSecurityException e) {
                            logger.e(TAG, e.getMessage(), e);
                        } catch (IOException e) {
                            logger.e(TAG, e.getMessage(), e);
                        }
                    } else {
                        logger.e(TAG, "Unable to pin SSL as requested. Will send data without SSL pinning.");
                    }
                    client.initializedSSLSocketFactory = true;
                }
            }
        });
        return this;
    }

    // why not override base method?
    @Override
    public synchronized AmplitudeClient initialize(Context context, String apiKey, String userId) {
        return initializeInternal(context, apiKey, userId, null);
    }

    public synchronized AmplitudeClient initialize(
            Context context,
            String apiKey,
            String userId,
            Provider<OkHttpClient> clientProvider) {
        return initializeInternal(context, apiKey, userId, clientProvider);
    }

    /**
     * Gets pinned cert ssl socket factory.
     *
     * @param serverZone the current server zone
     * @return the pinned cert ssl socket factory
     */
    protected SSLSocketFactory getPinnedCertSslSocketFactory(AmplitudeServerZone serverZone) {
        return getPinnedCertSslSocketFactory(getPinnedCertificateChain(serverZone));
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
                if (context.serverZone == AmplitudeServerZone.EU) {
                    logger.i(TAG, "Pinning SSL session using AWS Root CA Cert");
                } else {
                    logger.i(TAG, "Pinning SSL session using Comodo CA Cert");
                }
            } catch (Exception e) {
                logger.e(TAG, e.getMessage(), e);
            }
        }
        return sslSocketFactory;
    }

    @Override
    public AmplitudeClient setServerZone(AmplitudeServerZone serverZone) {
        super.setServerZone(serverZone);
        this.initializedSSLSocketFactory = false;
        this.sslSocketFactory = null;
        this.initialize(this.context, this.apiKey, this.userId);
        return this;
    }
}
