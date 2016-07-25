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

import com.markuspage.jbinrepoproxy.standalone.transport.TransactionInfo;
import com.markuspage.jbinrepoproxy.standalone.transport.TransportClient;
import com.markuspage.jbinrepoproxy.standalone.transport.TransportFetch;
import com.markuspage.jbinrepoproxy.standalone.transport.TransportHandler;
import com.markuspage.jbinrepoproxy.standalone.transport.TransportRequest;
import com.markuspage.jbinrepoproxy.standalone.transport.TransportResult;
import com.markuspage.jbinrepoproxy.standalone.transport.TransportServer;
import com.markuspage.jbinrepoproxy.standalone.transport.sun.SunTransportServer;
import com.markuspage.jbinrepoproxy.standalone.trust.ArtifactVerifier;
import com.markuspage.jbinrepoproxy.standalone.trust.ChecksumResult;
import com.markuspage.jbinrepoproxy.standalone.trust.ChecksumVerificationData;
import com.markuspage.jbinrepoproxy.standalone.trust.URIVerifier;
import com.markuspage.jbinrepoproxy.standalone.trust.KeysMap;
import com.markuspage.jbinrepoproxy.standalone.trust.SignatureResult;
import com.markuspage.jbinrepoproxy.standalone.trust.SignatureVerificationData;
import com.markuspage.jbinrepoproxy.standalone.trust.file.PropertiesFileTrustMap;
import com.markuspage.jbinrepoproxy.standalone.trust.s4u.PGPVerifyMavenPluginKeysMap;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import org.apache.http.HttpHost;
import org.bouncycastle.openpgp.PGPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standalone proxy application.
 *
 * @author Markus Kilås
 */
public class Main {

    /** Logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static volatile ArtifactVerifier artifactVerifier; // volatile in order to be updated by the file watch service

    /**
     * @param args the command line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: jbinrepoproxy-standalone <CONFIG FILE>");
            System.err.println("Example: jbinreporoxy-standalone conf/jbinrepoproxy-standalone.properties");
            System.exit(1);
        }

        // Configuration file
        final File file = new File(args[0]);
        final Configuration config = Configuration.fromFile(file);

        // Keys map
        final File trustMapFile = config.getTrustMapFile();
        LOG.info("Using keys map file {}", trustMapFile);
        
        // Trust map
        final PropertiesFileTrustMap trustMap = PropertiesFileTrustMap.fromFile(trustMapFile);
        LOG.info("Final trust map:\n{}", trustMap);

        // Target
        LOG.info("Binding to {} with port {}", config.getHost(), config.getPort());
        final HttpHost host = new HttpHost(config.getTargetHost(), config.getTargetPort(), config.getTargetScheme());
        LOG.info("Will proxy to {}", host);

        // Keys cache
        final KeysMap keysMap = new PGPVerifyMavenPluginKeysMap(config.getCacheKeysFolder(), config.getCacheKeysServer());

        // Verifier
        artifactVerifier = new ArtifactVerifier(trustMap, keysMap, true);

        // Start the server
        startServer(config.getHost(), config.getPort(), host);

        // Watch for changes in the trust directory and reload when needed
        runFileWatchService(config.getTrustMapFile(), keysMap);
    }
    
    private static void startServer(String hostname, int port, HttpHost host) throws IOException {
        TransportHandler handler = new TransportHandler() {

            private final TransactionLogger transactionLogger = new TransactionLogger();

            @Override
            public TransportResult handleRequest(String uri, TransportRequest request, TransportClient client, TransactionInfo transaction) {
                LOG.info("Is acceptable?: {}", uri);
                try {
                    final TransportResult result;

                    // Always accept signature files, digests, metadata, indexes and the root page
                    if (URIVerifier.isSafe(uri)) {
                        result = TransportResult.SUCCESS;
                    } else {
                        // TODO: Check local cache first
                        // We need to fetch the signature file
                        TransportFetch signatureFetch = client.httpGetOtherFile(uri + ".asc");
                        if (signatureFetch.getResponseCode() == 200) {
                            String signature = new String(signatureFetch.getContent());
                            LOG.debug(signature);

                            final SignatureResult signatureResult;
                            
                            TransportFetch theFetch = client.httpGetTheFile();
                            if (theFetch.getResponseCode() != 200) {
                                signatureResult = SignatureResult.SIGNATURE_MISSING;
                                result = new TransportResult(theFetch.getResponseCode(), theFetch.getErrorMessage());
                            } else {
                                SignatureVerificationData svd = artifactVerifier.verifyBySignature(uri, theFetch.getContent(), signature);
                                signatureResult = svd.getResult();
                                if (svd.isTrusted()) {
                                    result = TransportResult.SUCCESS;
                                } else {
                                    result = new TransportResult(403, "Not trusted signature: " + svd.getResult());
                                }
                            }
                            transactionLogger.setSignatureResult(signatureResult);
                        } else if (signatureFetch.getResponseCode() == 404) {
                            LOG.debug("Signature file not found, checking for trusted digest instead");
                            transactionLogger.setSignatureResult(SignatureResult.SIGNATURE_MISSING);
                            TransportFetch theFetch = client.httpGetTheFile();
                            final ChecksumResult checksumResult;
                            if (theFetch.getResponseCode() != 200) {
                                result = new TransportResult(theFetch.getResponseCode(), theFetch.getErrorMessage());
                                checksumResult = ChecksumResult.FAILED;
                            } else {
                                ChecksumVerificationData cvd = artifactVerifier.verifyByStoredChecksum(uri, theFetch.getContent());
                                checksumResult = cvd.getResult();
                                if (cvd.isTrusted()) {
                                    result = TransportResult.SUCCESS;
                                } else {
                                    result = new TransportResult(403, "Not trusted checksum: " + cvd.getResult());
                                }
                            }
                        } else {
                            transactionLogger.setSignatureResult(null);
                            result = new TransportResult(403, "Signature fetch failed (" + signatureFetch.getResponseCode() + "): " + signatureFetch.getErrorMessage());
                        }
                    }
                    return result;
                } catch (IOException ex) {
                    LOG.error("IO error", ex);
                    return new TransportResult(500, ex.getMessage());
                }
            }

            @Override
            public void finished(TransactionInfo transaction) {
                transactionLogger.log(transaction);
            }
        };

        // TODO: Determine which implementation to use and load using factory
        TransportServer server = new SunTransportServer(InetAddress.getByName(hostname), port, host.getSchemeName() + "://" + host.getHostName() + ":" + host.getPort());
        //TransportServer server = new HttpCoreTransportServer(hostname, port, host.getSchemeName(), host.getHostName(), host.getPort());
        server.start(handler);
    }

    private static void runFileWatchService(final File trustMapFile, final KeysMap keysMap) throws IOException, PGPException {
        WatchService watcher = FileSystems.getDefault().newWatchService();
        Path dir = trustMapFile.toPath().getParent();
        dir.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);

        for (;;) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException ex) {
                return;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                // Reload the keys map
                try {
                    artifactVerifier = new ArtifactVerifier(PropertiesFileTrustMap.fromFile(trustMapFile), keysMap, true);
                } catch (IOException ex) {
                    LOG.error("Failed to reload trust map: " + ex.getLocalizedMessage(), ex);
                    continue;
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        }
    }

}
