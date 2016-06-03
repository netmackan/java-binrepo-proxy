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

import static com.markuspage.jbinrepoproxy.standalone.transport.httpcore.HttpCoreTransportServer.HTTP_CONN_KEEPALIVE;
import com.markuspage.jbinrepoproxy.standalone.transport.spi.TransportClient;
import com.markuspage.jbinrepoproxy.standalone.transport.spi.TransportFetch;
import java.io.IOException;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TransportClient implementation using HttpCore.
 *
 * @author Markus Kilås
 */
public class HttpCoreTransportClientImpl implements TransportClient {

    /** Logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(HttpCoreTransportClientImpl.class);
    
    private final HttpRequestExecutor httpexecutor;
    private final HttpProcessor httpproc;
    private final ConnectionReuseStrategy connStrategy;
    private final HttpClientConnection conn;
    private final HttpRequest request;
    private final HttpResponse response;
    private final HttpContext context;
    
    private HttpResponse targetResponse;
    private byte[] targetBody;

    public HttpCoreTransportClientImpl(HttpRequestExecutor httpexecutor, HttpProcessor httpproc, ConnectionReuseStrategy connStrategy, final HttpClientConnection conn, final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) {
        this.httpexecutor = httpexecutor;
        this.httpproc = httpproc;
        this.connStrategy = connStrategy;
        this.conn = conn;
        this.request = request;
        this.response = response;
        this.context = context;
    }

    protected HttpResponse getTargetResponse() {
        return targetResponse;
    }

    protected byte[] getTargetBody() {
        return targetBody;
    }
    
    @Override
    public TransportFetch httpGetTheFile() {
        try {
            httpexecutor.preProcess(request, httpproc, context);
            final HttpResponse targetResponse = httpexecutor.execute(request, conn, context);
            httpexecutor.postProcess(response, httpproc, context);
            this.targetResponse = targetResponse;

            final byte[] targetBody = EntityUtils.toByteArray(targetResponse.getEntity());
            LOG.info("Read body of {} bytes", targetBody.length);
            EntityUtils.consume(targetResponse.getEntity());
            this.targetBody = targetBody;

            boolean keepalive = connStrategy.keepAlive(response, context);
            context.setAttribute(HTTP_CONN_KEEPALIVE, new Boolean(keepalive));

            return new TransportFetch(targetResponse.getStatusLine().getStatusCode(), targetResponse.getStatusLine().getReasonPhrase(), targetBody);
        } catch (HttpException | IOException ex) {
            return new TransportFetch(500, ex.getLocalizedMessage(), null);
        }
    }

    @Override
    public TransportFetch httpGetOtherFile(String uri) {
        HttpProcessor httpproc = HttpProcessorBuilder.create()
        .add(new RequestContent())
        .add(new RequestTargetHost())
        .add(new RequestConnControl())
        .add(new RequestUserAgent("Test/1.1"))
        .add(new RequestExpectContinue(true)).build();

        try {
            HttpRequest ascRequest = new BasicHttpRequest("GET", uri);
            LOG.info("Will fetch {}", ascRequest.getRequestLine());
            httpexecutor.preProcess(ascRequest, httpproc, context);
            final HttpResponse otherResponse = httpexecutor.execute(ascRequest, conn, context);
            httpexecutor.postProcess(response, httpproc, context);

            final byte[] otherBody = EntityUtils.toByteArray(otherResponse.getEntity());
            LOG.info("Read body of {} bytes", otherBody.length, " bytes");
            EntityUtils.consume(otherResponse.getEntity());

            boolean keepalive = connStrategy.keepAlive(response, context);
            context.setAttribute(HTTP_CONN_KEEPALIVE, new Boolean(keepalive));

            return new TransportFetch(otherResponse.getStatusLine().getStatusCode(), otherResponse.getStatusLine().getReasonPhrase(), otherBody);
        } catch (HttpException | IOException ex) {
            return new TransportFetch(500, ex.getLocalizedMessage(), null);
        }
    }

}
