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

import com.markuspage.jbinrepoproxy.standalone.Main;
import com.markuspage.jbinrepoproxy.standalone.transport.spi.TransportClient;
import com.markuspage.jbinrepoproxy.standalone.transport.spi.TransportFetch;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

/**
 * TransportClient implementation using the standard Java URLConnection.
 *
 * @author Markus Kilås
 */
public class URLConnectionTransportClientImpl implements TransportClient{

    /** Logger for this class. */
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(Main.class);

    private final String serverURL;
    private final String theFileURI;
    private TargetResponse targetResponse;

    public URLConnectionTransportClientImpl(String serverURL, String theFileURI) {
        this.serverURL = serverURL;
        this.theFileURI = theFileURI;
    }

    @Override
    public TransportFetch httpGetTheFile() {
        TransportFetch result;
        try {
            final URL url = new URL(serverURL + theFileURI);

            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setAllowUserInteraction(false);
            final int responseCode = conn.getResponseCode();
            final String responseMessage = conn.getResponseMessage();
            InputStream responseIn = conn.getErrorStream();
            if (responseIn == null) {
                responseIn = conn.getInputStream();
            }

            // Read the body to the output if OK otherwise to the error message
            final byte[] body = IOUtils.toByteArray(responseIn);

            this.targetResponse = new TargetResponse(responseCode, conn.getHeaderFields(), body);

            result = new TransportFetch(responseCode, responseMessage, body);
        } catch (MalformedURLException ex) {
            LOG.error("Malformed URL", ex);
            result = new TransportFetch(500, ex.getLocalizedMessage(), null);
        } catch (IOException ex) {
            LOG.error("IO error", ex);
            result = new TransportFetch(500, ex.getLocalizedMessage(), null);
        }

        return result;
    }

    @Override
    public TransportFetch httpGetOtherFile(String uri) {
        TransportFetch result;
        try {
            final URL url = new URL(serverURL + uri);

            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setAllowUserInteraction(false);
            final int responseCode = conn.getResponseCode();
            final String responseMessage = conn.getResponseMessage();
            InputStream responseIn = conn.getErrorStream();
            if (responseIn == null) {
                responseIn = conn.getInputStream();
            }

            // Read the body to the output if OK otherwise to the error message
            final byte[] body = IOUtils.toByteArray(responseIn);

            result = new TransportFetch(responseCode, responseMessage, body);
        } catch (MalformedURLException ex) {
            LOG.error("Malformed URL", ex);
            result = new TransportFetch(500, ex.getLocalizedMessage(), null);
        } catch (IOException ex) {
            LOG.error("IO error", ex);
            result = new TransportFetch(500, ex.getLocalizedMessage(), null);
        }

        return result;
    }

    TargetResponse getTargetResponse() {
        return targetResponse;
    }

}
