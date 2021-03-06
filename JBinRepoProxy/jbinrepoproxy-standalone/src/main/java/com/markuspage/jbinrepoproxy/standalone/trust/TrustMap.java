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

import java.util.Collection;
import org.bouncycastle.openpgp.PGPPublicKey;

/**
 * Maps the trust for URIs from public keys or stored checksums.
 *
 * @author Markus Kilås
 */
public interface TrustMap {

    boolean isKeyTrustedForURI(String uri, PGPPublicKey publicKey);

    boolean isTrustedChecksumStoredForURI(String uri, byte[] data);

    Collection<String> getTrustedURIs(PGPPublicKey publicKey);
    
}
