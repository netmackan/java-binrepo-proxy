/*
 * Copyright (C) 2016 Markus Kilås
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.markuspage.jbinrepoproxy.standalone;

import com.github.s4u.plugins.PGPKeysCache;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import org.apache.http.examples.StartableReverseProxy;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.examples.ElementalReverseProxy;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;

/**
 * Standalone proxy application.
 *
 * @author Markus Kilås
 */
public class Main {

    private static final Log LOG = LogFactory.getLog(Main.class);
    
    private static final Map<Integer, String> WEAK_SIGALGS = new HashMap<>();

    static {
        WEAK_SIGALGS.put(1, "MD5");
        WEAK_SIGALGS.put(4, "DOUBLE_SHA");
        WEAK_SIGALGS.put(5, "MD2");
        WEAK_SIGALGS.put(6, "TIGER_192");
        WEAK_SIGALGS.put(7, "HAVAL_5_160");
        WEAK_SIGALGS.put(11, "SHA224");
    }
    
    private static final boolean failWeakSignature = true;
    
    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException, URISyntaxException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, FileNotFoundException, PGPException {
        if (args.length < 5) {
            System.err.println("Usage: jbinrepoproxy-standalone <CONFIG FILE> <PORT> <TARGET HOSTNAME> <TARGET PORT> <TARGET SCHEME>");
            System.err.println("Example 1: jbinreporoxy-standalone trust/keysmap.properties 8888 repo1.example.com 80 http");
            System.err.println("Example 2: jbinreporoxy-standalone trust/keysmap.properties 8888 repo1.example.com 443 https");
            System.exit(1);
        }
        final File keysmapFile = new File(args[0]);
        final int port = Integer.parseInt(args[1]);
        final String targetHost = args[2];
        final int targetPort = Integer.parseInt(args[3]);
        final String targetScheme = args[4];
        
        // Keys map
        System.out.println("Using keys map file " + keysmapFile.getAbsolutePath());
        final KeysMap keysMap = new KeysMap();
        keysMap.load(keysmapFile);
        keysMap.print();

        System.out.println("Will proxy to " + targetScheme + "://" + targetHost + ":" + targetPort);
        
        // Keys cache
        File cachePath = new File("/tmp/"); // TODO
        String keyServer = "hkps://hkps.pool.sks-keyservers.net"; // TODO
        final PGPKeysCache pgpKeysCache = new PGPKeysCache(LOG, cachePath, keyServer);

        new StartableReverseProxy().start(port, new HttpHost(targetHost, targetPort, targetScheme), new ElementalReverseProxy.RequestFilter() {
            @Override
            public boolean isAcceptable(HttpRequest request, HttpResponse targetResponse, byte[] targetBody, HttpClientConnection conn1, HttpContext context, HttpProcessor httpproc1, HttpRequestExecutor httpexecutor1, ConnectionReuseStrategy connStrategy1) throws HttpException, IOException {
                final String uri = request.getRequestLine().getUri();
                System.out.println("Is acceptable?: " + uri);
                boolean results;
                
                // Always accept signature files and digests
                if (targetResponse.getStatusLine().getStatusCode() == 404) {
                    System.out.println("File not found: " + uri);
                    results = false;
                } else if (uri.endsWith(".asc") || uri.endsWith(".sha1") || uri.endsWith(".sha256")) {
                    results = true;
                } else {
                    HttpProcessor httpproc = HttpProcessorBuilder.create()
                    .add(new RequestContent())
                    .add(new RequestTargetHost())
                    .add(new RequestConnControl())
                    .add(new RequestUserAgent("Test/1.1"))
                    .add(new RequestExpectContinue(true)).build();

                    try {
                        HttpRequest ascRequest = new BasicHttpRequest("GET", request.getRequestLine().getUri() + ".asc");
                        System.out.println("Will fetch " + ascRequest.getRequestLine());
                        httpexecutor1.preProcess(ascRequest, httpproc, context);
                        HttpResponse ascResponse = httpexecutor1.execute(ascRequest, conn1, context);
                        httpexecutor1.postProcess(ascResponse, httpproc, context);
                        
                        System.out.println("<< asc response: " + ascResponse.getStatusLine());
                        if (ascResponse.getStatusLine().getStatusCode() == 404) {
                            System.err.println("Signature file not found, checking for trusted digest instead");
                            results = verifyDigest(uri, targetBody);
                        } else {
                            String signature = EntityUtils.toString(ascResponse.getEntity());
                            System.out.println(signature);
                            results = verifyPGPSignature(uri, targetBody, signature);
                        }
                        
                        final boolean keepalive = connStrategy1.keepAlive(ascResponse, context);
                        context.setAttribute(ElementalReverseProxy.HTTP_CONN_KEEPALIVE, keepalive);
                    } catch (HttpException | IOException ex) {
                        ex.printStackTrace();
                        results = false;
                    }
                }

                return results;
            }
            
            private boolean verifyPGPSignature(String uri, byte[] data, String signature) throws IOException {
                try {
                    InputStream sigInputStream = PGPUtil.getDecoderStream(new ByteArrayInputStream(signature.getBytes("ASCII")));
                    PGPObjectFactory pgpObjectFactory = new PGPObjectFactory(sigInputStream, new BcKeyFingerprintCalculator());
                    PGPSignatureList sigList = (PGPSignatureList) pgpObjectFactory.nextObject();
                    if (sigList == null) {
                        throw new IOException("Missing signature");
                    }
                    PGPSignature pgpSignature = sigList.get(0);
                    
                    PGPPublicKey publicKey = pgpKeysCache.getKey(pgpSignature.getKeyID());
                    
                    if (!keysMap.isValidKey(uri, publicKey)) {
                        String msg = String.format("%s=0x%X", uri, publicKey.getKeyID());
                        String keyUrl = pgpKeysCache.getUrlForShowKey(publicKey.getKeyID());
                        getLog().error(String.format("Not allowed artifact %s and keyID:\n\t%s\n\t%s\n", uri, msg, keyUrl));
                        return false;
                    }
                    
                    pgpSignature.init(new BcPGPContentVerifierBuilderProvider(), publicKey);
                    pgpSignature.update(data);
                    
                    String msgFormat = "%s PGP Signature %s\n       KeyId: 0x%X UserIds: %s";
                    if (pgpSignature.verify()) {
                        getLog().info(String.format(msgFormat, uri,
                                "OK", publicKey.getKeyID(), Arrays.asList(publicKey.getUserIDs())));
                        if (WEAK_SIGALGS.containsKey(pgpSignature.getHashAlgorithm())) {
                            if (failWeakSignature) {
                                getLog().error("Weak signature algorithm used: "
                                        + WEAK_SIGALGS.get(pgpSignature.getHashAlgorithm()));
                                return false;
                            } else {
                                getLog().warn("Weak signature algorithm used: "
                                        + WEAK_SIGALGS.get(pgpSignature.getHashAlgorithm()));
                            }
                        }
                        return true;
                    } else {
                        getLog().warn(String.format(msgFormat, uri,
                                "ERROR", publicKey.getKeyID(), Arrays.asList(publicKey.getUserIDs())));
                        getLog().warn(uri);
                        return false;
                    }
                } catch (PGPException ex) {
                    throw new IOException(ex);
                }
            }
            
            private boolean verifyDigest(String uri, byte[] data) {
                return keysMap.isValidDigest(uri, data);
            }

            private Log getLog() {
                return LOG;
            }
        });
    }
    
}
