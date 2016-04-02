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

import org.apache.http.examples.StartableReverseProxy;
import java.io.IOException;
import java.net.Socket;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.examples.ElementalReverseProxy;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;

/**
 * Standalone proxy application.
 *
 * @author Markus Kilås
 */
public class Main {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: jbinrepoproxy-standalone <PORT> <TARGET HOSTNAME> [TARGET PORT]");
            System.err.println("Example: jbinreporoxy-standalone 8888 repo1.example.com 80");
            System.exit(1);
        }
        final int port = Integer.parseInt(args[0]);
        final String targetHost = args[1];
        int targetPort = 80;
        if (args.length > 1) {
            targetPort = Integer.parseInt(args[2]);
        }
        final int ttargetPort = targetPort;
        System.out.println("Will proxy to " + targetHost + ":" + targetPort);
        new StartableReverseProxy().start(port, new HttpHost(targetHost, targetPort), new ElementalReverseProxy.RequestFilter() {
            @Override
            public boolean isAcceptable(HttpRequest request, HttpResponse targetResponse, byte[] targetBody, HttpClientConnection conn1, HttpContext context, HttpProcessor httpproc1, HttpRequestExecutor httpexecutor1, ConnectionReuseStrategy connStrategy1) throws HttpException, IOException {
                final String uri = request.getRequestLine().getUri();
                System.out.println("Is acceptable?: " + uri);
                final boolean results;
                
                // Always accept signature files
                if (uri.endsWith(".asc")) {
                    results = true;
                } else {
                    
                    //TODO: I want to re-use the existing connection
                
                    HttpProcessor httpproc = HttpProcessorBuilder.create()
                    .add(new RequestContent())
                    .add(new RequestTargetHost())
                    .add(new RequestConnControl())
                    .add(new RequestUserAgent("Test/1.1"))
                    .add(new RequestExpectContinue(true)).build();

                    HttpRequestExecutor httpexecutor = new HttpRequestExecutor();

                    HttpCoreContext coreContext = HttpCoreContext.create();
                    HttpHost host = new HttpHost(targetHost, ttargetPort);
                    coreContext.setTargetHost(host);

                    DefaultBHttpClientConnection conn = new DefaultBHttpClientConnection(8 * 1024);
                    ConnectionReuseStrategy connStrategy = DefaultConnectionReuseStrategy.INSTANCE;

                    if (!conn.isOpen()) {
                        Socket socket = new Socket(host.getHostName(), host.getPort());
                        conn.bind(socket);
                    }
                    
                    BasicHttpRequest ascRequest = new BasicHttpRequest("GET", request.getRequestLine().getUri() + ".asc");
                    System.out.println("Will fetch " + ascRequest.getRequestLine());
                    httpexecutor.preProcess(ascRequest, httpproc, context);
                    HttpResponse ascResponse = httpexecutor.execute(ascRequest, conn, context);
                    httpexecutor.postProcess(ascResponse, httpproc, context);

                    System.out.println("<< asc response: " + ascResponse.getStatusLine());
                    System.out.println(EntityUtils.toString(ascResponse.getEntity()));
                    
                    // TODO: Check the signature here
                    
                    
                    System.out.println("==============");
                    if (!connStrategy.keepAlive(ascResponse, context)) {
                        conn.close();
                    } else {
                        System.out.println("Connection kept alive...");
                    }
            
                    conn.close();
                    
                    results = true;
                }
                
                
                return results;
            }
        });
    }

}
