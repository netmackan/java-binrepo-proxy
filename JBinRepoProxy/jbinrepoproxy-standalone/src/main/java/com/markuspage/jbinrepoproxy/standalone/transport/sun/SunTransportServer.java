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
package com.markuspage.jbinrepoproxy.standalone.transport.sun;

import com.markuspage.jbinrepoproxy.standalone.transport.TransactionInfo;
import com.markuspage.jbinrepoproxy.standalone.transport.TransportHandler;
import com.markuspage.jbinrepoproxy.standalone.transport.TransportRequest;
import com.markuspage.jbinrepoproxy.standalone.transport.TransportResult;
import com.markuspage.jbinrepoproxy.standalone.transport.TransportServer;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TransportServer implementation using com.sun.net.HttpServer for server and
 * and standard Java support for the client parts.
 *
 * @author Markus Kilås
 */
public class SunTransportServer implements TransportServer {

    /** Logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(SunTransportServer.class);
    
    public static final String CONTENT_LEN  = "Content-Length";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONN_DIRECTIVE = "Connection";
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";

    private final InetAddress bindAddress;
    private final int port;
    private final int backlog;
    private final int threads;
    private final String targetURL;

    private HttpServer server;

    public SunTransportServer(InetAddress bindAddress, int port, String targetURL) {
        this.bindAddress = bindAddress;
        this.port = port;
        this.backlog = 0;
        this.threads = 0;
        this.targetURL = targetURL;
    }

    @Override
    public void start(TransportHandler handler) throws IOException {
        InetSocketAddress addr = new InetSocketAddress(bindAddress, port);
        server = HttpServer.create(addr, backlog);

        server.createContext("/", (HttpExchange exchange) -> {
            final String uri = exchange.getRequestURI().toString();
            final TransactionInfo transaction = new TransactionInfo(exchange.getProtocol(), exchange.getRequestMethod(), uri);
            try {
                TransportRequest transportRequest = new TransportRequest();    
                LOG.info(">> Request URI: {}", uri);
                URLConnectionTransportClientImpl transportClient = new URLConnectionTransportClientImpl(targetURL, uri);
                TransportResult result = handler.handleRequest(uri, transportRequest, transportClient, transaction);
                final int responseCode;
                if (result.getResponseCode() != 200) {
                    responseCode = 403;
                    exchange.sendResponseHeaders(responseCode, -1); // TODO: How to send a response message?
                    // TODO: What would be appropriate response.setHeaders();
                } else {
                    responseCode = result.getResponseCode();
                    // If the user did not fetch the file, do it now
                    if (transportClient.getTargetResponse() == null) {
                        transportClient.httpGetTheFile();
                    }

                    TargetResponse targetResponse = transportClient.getTargetResponse();

                    // Remove hop-by-hop headers
                    Headers headers = new Headers();
                    headers.putAll(targetResponse.getHeaders());
                    headers.remove(null);
                    headers.remove(CONTENT_LEN);
                    headers.remove(TRANSFER_ENCODING);
                    headers.remove(CONN_DIRECTIVE);
                    headers.remove("Keep-Alive");
                    headers.remove("TE");
                    headers.remove("Trailers");
                    headers.remove("Upgrade");
                    exchange.getResponseHeaders().putAll(headers);

                    // Send response
                    byte[] body = targetResponse.getBody();
                    exchange.sendResponseHeaders(responseCode, body.length);
                    exchange.getResponseBody().write(body);
                    transaction.setContentLength(body.length);
                }
                transaction.setResponseCode(responseCode);

                LOG.info("<< Response: {}", responseCode);
            } catch (IOException ex) {
                transaction.setException(ex);
                throw new IOException(ex);
            } finally {
                handler.finished(transaction);
            }
        });

        if (threads == 0) {
            server.setExecutor(Executors.newCachedThreadPool());
        } else {
            server.setExecutor(Executors.newFixedThreadPool(threads));
        }
        server.start();
    }

}
