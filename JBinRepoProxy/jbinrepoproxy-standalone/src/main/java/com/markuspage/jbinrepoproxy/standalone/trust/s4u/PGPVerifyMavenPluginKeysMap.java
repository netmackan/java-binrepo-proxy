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
package com.markuspage.jbinrepoproxy.standalone.trust.s4u;

import com.github.s4u.plugins.PGPKeysCache;
import com.markuspage.jbinrepoproxy.standalone.trust.KeysMap;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;

/**
 * Implements the KeysMap by using the PGPKeysCache from s4u/pgp-verify-plugin.
 *
 * @author Markus Kilås
 */
public class PGPVerifyMavenPluginKeysMap implements KeysMap {

    private final PGPKeysCache delegate;
    
    public PGPVerifyMavenPluginKeysMap(File cacheKeysFolder, String cacheKeysServer) throws URISyntaxException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {
        this.delegate = new PGPKeysCache(LogFactory.getLog(PGPVerifyMavenPluginKeysMap.class), cacheKeysFolder, cacheKeysServer);
    }

    @Override
    public PGPPublicKey getKey(long keyID) throws IOException, PGPException {
        return delegate.getKey(keyID);
    }
    
}
