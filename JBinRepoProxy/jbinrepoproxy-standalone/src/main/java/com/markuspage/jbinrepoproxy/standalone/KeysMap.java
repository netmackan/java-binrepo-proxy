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

import java.util.Locale;
import java.util.Properties;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.util.encoders.Hex;

/**
 *
 * @author Markus Kilås
 */
public class KeysMap {

    private final Properties properties;

    public KeysMap(Properties properties) {
        this.properties = properties;
    }

    public boolean isValidKey(String uri, PGPPublicKey publicKey) {
        System.out.println("isValidKey(" + uri + ", " + String.format("0x%X", publicKey.getKeyID()) + " ?");
        String fingerprint = Hex.toHexString(publicKey.getFingerprint()).toUpperCase(Locale.US);
        System.out.println("fingerprint: " + fingerprint);
        String allowedPath = properties.getProperty(fingerprint);
        
        if (allowedPath == null) {
            System.err.println("No entry for " + fingerprint);
            return false;
        } else {
            return uri.startsWith(allowedPath);
        }
        
    }
}
