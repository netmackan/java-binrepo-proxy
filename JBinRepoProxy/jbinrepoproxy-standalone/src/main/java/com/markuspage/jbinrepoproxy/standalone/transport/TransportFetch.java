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
package com.markuspage.jbinrepoproxy.standalone.transport;

/**
 * Results of attempting to fetch a file.
 *
 * @author Markus Kilås
 */
public class TransportFetch {
    private final int responseCode;
    private final String errorMessage;
    private final byte[] content;

    public TransportFetch(int responseCode, String errorMessage, byte[] content) {
        this.responseCode = responseCode;
        this.errorMessage = errorMessage;
        this.content = content;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public byte[] getContent() {
        return content;
    }

}
