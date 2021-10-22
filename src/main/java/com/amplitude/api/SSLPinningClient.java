package com.amplitude.api;

import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

class SSLPinningClient extends HttpClient {

    private static final String TAG = SSLPinningClient.class.getName();
    private static final AmplitudeLog logger = AmplitudeLog.getLogger();

    private SSLSocketFactory sslSocketFactory;

    private static final String CERTIFICATE_1 = ""
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

    protected static final SSLContextBuilder SSL_CONTEXT_API_AMPLITUDE_COM =
            new SSLContextBuilder().addCertificate(CERTIFICATE_1);

    public SSLPinningClient(String apiKey, String url, String bearerToken) {
        super(apiKey, url, bearerToken);
    }

    @Override
    public HttpURLConnection getNewConnection(String url) throws IOException {
        try {
            URL urlObject = new URL(url);
            HttpsURLConnection connection = (HttpsURLConnection) urlObject.openConnection();
            connection.setSSLSocketFactory(getPinnedCertSslSocketFactory());
            return connection;
        } catch (ClassCastException | MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL, must establish HTTPS connection on PinnedAmplitudeClient for SSL features");
        }
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
                    byte[] decodedCert = Base64.decode(certificateBase64, Base64.DEFAULT);
                    InputStream stream = new ByteArrayInputStream(decodedCert);
                    X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(stream);
                    keyStore.setCertificateEntry(Integer.toString(nextName++), certificate);
                }

                // Create an SSL context that uses these certificates as its trust store.
                trustManagerFactory.init(keyStore);
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
                return sslContext;
            } catch (GeneralSecurityException | IOException e) {
                logger.e(TAG, e.getMessage(), e);
            }
            return null;
        }
    }
}
