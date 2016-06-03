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
package com.markuspage.jbinrepoproxy.standalone.transport.httpcore;

import com.markuspage.jbinrepoproxy.standalone.transport.spi.TransportHandler;
import com.markuspage.jbinrepoproxy.standalone.transport.spi.TransportRequest;
import com.markuspage.jbinrepoproxy.standalone.transport.spi.TransportResult;
import com.markuspage.jbinrepoproxy.standalone.transport.spi.TransportServer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.examples.ElementalReverseProxy;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TransportServer implementation using HttpCore for server and client parts.
 *
 * @author Markus Kilås
 */
public class HttpCoreTransportServer implements TransportServer {
    
    /** Logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(HttpCoreTransportServer.class);

    private static final String HTTP_OUT_CONN = "http.proxy.out-conn";
    public static final String HTTP_CONN_KEEPALIVE = "http.proxy.conn-keepalive";

    private final HttpHost target;

    public HttpCoreTransportServer(String bindHost, int bindPort, String targetScheme, String targetHost, int targetPort) {
        this.target = new HttpHost(targetHost, targetPort, targetScheme);
    }

    @Override
    public void start(TransportHandler handler) throws IOException {
        final Thread t = new RequestListenerThread(8888, target, handler);
        t.setDaemon(false);
        t.start();
    }

    static class RequestListenerThread extends Thread {

        private final HttpHost target;
        private final ServerSocket serversocket;
        private final HttpService httpService;

        public RequestListenerThread(final int port, final HttpHost target, final TransportHandler handler) throws IOException {
            this.target = target;
            this.serversocket = new ServerSocket(port);

            // Set up HTTP protocol processor for incoming connections
            final HttpProcessor inhttpproc = new ImmutableHttpProcessor(
                    new HttpRequestInterceptor[] {
                            new RequestContent(),
                            new RequestTargetHost(),
                            new RequestConnControl(),
                            new RequestUserAgent("Test/1.1"),
                            new RequestExpectContinue(true)
             });

            // Set up HTTP protocol processor for outgoing connections
            final HttpProcessor outhttpproc = new ImmutableHttpProcessor(
                    new HttpResponseInterceptor[] {
                            new ResponseDate(),
                            new ResponseServer("Test/1.1"),
                            new ResponseContent(),
                            new ResponseConnControl()
            });

            // Set up outgoing request executor
            final HttpRequestExecutor httpexecutor = new HttpRequestExecutor();

            // Set up incoming request handler
            final UriHttpRequestHandlerMapper reqistry = new UriHttpRequestHandlerMapper();
            reqistry.register("*", new ProxyHandler(
                    this.target,
                    outhttpproc,
                    httpexecutor,
                    handler));

            // Set up the HTTP service
            this.httpService = new HttpService(inhttpproc, reqistry);
        }

        @Override
        public void run() {
            LOG.info("Listening on port {}", this.serversocket.getLocalPort());
            while (!Thread.interrupted()) {
                try {
                    final int bufsize = 8 * 1024;
                    // Set up incoming HTTP connection
                    final Socket insocket = this.serversocket.accept();
                    final DefaultBHttpServerConnection inconn = new DefaultBHttpServerConnection(bufsize);
                    System.out.println("Incoming connection from " + insocket.getInetAddress());
                    inconn.bind(insocket);

                    // Set up outgoing HTTP connection
                    final Socket outsocket;
                    if (this.target.getSchemeName().equals("https")) {
                        SSLContext sslcontext = SSLContexts.createSystemDefault();
                        SocketFactory sf = sslcontext.getSocketFactory();
                        outsocket = (SSLSocket) sf.createSocket(this.target.getHostName(), 443);
    //                    // Enforce TLS and disable SSL
    //                    socket.setEnabledProtocols(new String[] {
    //                            "TLSv1",
    //                            "TLSv1.1",
    //                            "TLSv1.2" });
    //                    // Enforce strong ciphers
    //                    socket.setEnabledCipherSuites(new String[] {
    //                            "TLS_RSA_WITH_AES_256_CBC_SHA",
    //                            "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
    //                            "TLS_DHE_DSS_WITH_AES_256_CBC_SHA" });
                    } else {
                        outsocket = new Socket(this.target.getHostName(), this.target.getPort());
                    }
                    final DefaultBHttpClientConnection outconn = new DefaultBHttpClientConnection(bufsize);
                    outconn.bind(outsocket);
                    LOG.info("Outgoing connection to {}", outsocket.getInetAddress());

                    // Start worker thread
                    final Thread t = new ElementalReverseProxy.ProxyThread(this.httpService, inconn, outconn);
                    t.setDaemon(true);
                    t.start();
                } catch (final InterruptedIOException ex) {
                    break;
                } catch (final IOException e) {
                    LOG.error("I/O error initialising connection thread: {}",
                            e.getMessage());
                    break;
                }
            }
        }
    }

    static class ProxyHandler implements HttpRequestHandler  {

        private final HttpHost target;
        private final HttpProcessor httpproc;
        private final HttpRequestExecutor httpexecutor;
        private final ConnectionReuseStrategy connStrategy;
        private final TransportHandler handler;

        public ProxyHandler(
                final HttpHost target,
                final HttpProcessor httpproc,
                final HttpRequestExecutor httpexecutor,
                final TransportHandler handler) {
            super();
            this.target = target;
            this.httpproc = httpproc;
            this.httpexecutor = httpexecutor;
            this.connStrategy = DefaultConnectionReuseStrategy.INSTANCE;
            this.handler = handler;
        }

        @Override
        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {

            final HttpClientConnection conn = (HttpClientConnection) context.getAttribute(
                    HTTP_OUT_CONN);

            context.setAttribute(HttpCoreContext.HTTP_CONNECTION, conn);
            context.setAttribute(HttpCoreContext.HTTP_TARGET_HOST, this.target);

            final String uri = request.getRequestLine().getUri();
            LOG.info(">> Request URI: {}", uri);

            // Remove hop-by-hop headers
            request.removeHeaders(HTTP.CONTENT_LEN);
            request.removeHeaders(HTTP.TRANSFER_ENCODING);
            request.removeHeaders(HTTP.CONN_DIRECTIVE);
            request.removeHeaders("Keep-Alive");
            request.removeHeaders("Proxy-Authenticate");
            request.removeHeaders("TE");
            request.removeHeaders("Trailers");
            request.removeHeaders("Upgrade");

            // JBinRepoProxy: Modified to be able to run from localhost without using a different hostname
            // Sets the request hostname to the hostname of the server
            final String host = request.getFirstHeader("Host").getValue();
            if (host != null && (host.startsWith("localhost:") || host.equals("localhost"))) {
                request.setHeader("Host", target.getHostName());
            }

            TransportRequest transportRequest = new TransportRequest();
            HttpCoreTransportClientImpl transportClient = new HttpCoreTransportClientImpl(httpexecutor, httpproc, connStrategy, conn, request, response, context);

            TransportResult result = handler.handleRequest(uri, transportRequest, transportClient);
            if (result.getResponseCode() != 200) {
                response.setStatusLine(request.getProtocolVersion(), 403, "Forbidden by proxy");
                // TODO: What would be appropriate response.setHeaders();
                response.setEntity(null);
            } else {
                // If the user did not fetch the file, do it now
                if (transportClient.getTargetResponse() == null) {
                    transportClient.httpGetTheFile();
                }

                // Remove hop-by-hop headers
                transportClient.getTargetResponse().removeHeaders(HTTP.CONTENT_LEN);
                transportClient.getTargetResponse().removeHeaders(HTTP.TRANSFER_ENCODING);
                transportClient.getTargetResponse().removeHeaders(HTTP.CONN_DIRECTIVE);
                transportClient.getTargetResponse().removeHeaders("Keep-Alive");
                transportClient.getTargetResponse().removeHeaders("TE");
                transportClient.getTargetResponse().removeHeaders("Trailers");
                transportClient.getTargetResponse().removeHeaders("Upgrade");

                response.setStatusLine(transportClient.getTargetResponse().getStatusLine());
                response.setHeaders(transportClient.getTargetResponse().getAllHeaders());
                //response.setEntity(targetResponse.getEntity());
                BasicHttpEntity responseEntity = new BasicHttpEntity();
                responseEntity.setContent(new ByteArrayInputStream(transportClient.getTargetBody()));
                responseEntity.setContentLength(transportClient.getTargetBody().length);
                response.setEntity(responseEntity);
            }

            LOG.info("<< Response: {}", response.getStatusLine());
        }

    }

}
