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
package com.markuspage.jbinrepoproxy.standalone.trust;

import java.net.MalformedURLException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tells if an URI is safe to return without checking for a signature.
 *
 * @author Markus Kilås
 */
public class URIVerifier {
    
    /** Logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(URIVerifier.class);
    
    public static boolean isSafe(String uri) {
        boolean result;
        try {
            // Parse as URL
            final URL url = new URL("http://example.com" + uri);
            final String path = url.getPath();
            result = path.endsWith(".asc") // Signature
                    || path.endsWith(".sha1") || path.endsWith(".sha256") // Digests
                    || path.startsWith("/maven2/.meta/") || path.startsWith("/maven2/.index/") // Metadata and indexes
                    || path.endsWith("/maven-metadata.xml") // Repository metadata
                    || path.endsWith("/"); // Index page
        } catch (MalformedURLException ex) {
            result = false;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Malformed URI: " + uri, ex);
            }
        }
        return result;
    }
}
