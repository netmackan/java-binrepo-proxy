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
package com.markuspage.jbinrepoproxy.standalone.transport.spi;

/**
 *
 * @author Markus Kilås
 */
public class TransactionInfo {

    private final String protocol;
    private final String requestMethod;
    private final String uri;
    private long contentLength;
    private int responseCode;
    private Exception exception;
    
    public TransactionInfo(String uri) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public TransactionInfo(String protocol, String requestMethod, String uri) {
        this.protocol = protocol;
        this.requestMethod = requestMethod;
        this.uri = uri;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public String getUri() {
        return uri;
    }
    
    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    
    
}
