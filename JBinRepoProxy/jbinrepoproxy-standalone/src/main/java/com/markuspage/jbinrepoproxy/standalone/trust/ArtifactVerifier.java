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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Markus Kilås
 */
public class ArtifactVerifier {
    
    /** Logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(ArtifactVerifier.class);
    
    private static final Map<Integer, String> WEAK_SIGALGS = new HashMap<>();
    
    static {
        WEAK_SIGALGS.put(1, "MD5");
        WEAK_SIGALGS.put(4, "DOUBLE_SHA");
        WEAK_SIGALGS.put(5, "MD2");
        WEAK_SIGALGS.put(6, "TIGER_192");
        WEAK_SIGALGS.put(7, "HAVAL_5_160");
        WEAK_SIGALGS.put(11, "SHA224");
    }
    
    private final TrustMap trustMap;
    private final KeysMap keysMap;
    private final boolean failWeakSignature;

    public ArtifactVerifier(TrustMap trustMap, KeysMap keysMap, boolean failWeakSignature) {
        this.trustMap = trustMap;
        this.keysMap = keysMap;
        this.failWeakSignature = failWeakSignature;
    }

    public boolean verifyBySignature(String uri, byte[] data, String signature) throws IOException {
        try {
            InputStream sigInputStream = PGPUtil.getDecoderStream(new ByteArrayInputStream(signature.getBytes("ASCII")));
            PGPObjectFactory pgpObjectFactory = new PGPObjectFactory(sigInputStream, new BcKeyFingerprintCalculator());
            PGPSignatureList sigList = (PGPSignatureList) pgpObjectFactory.nextObject();
            if (sigList == null) {
                throw new IOException("Missing signature");
            }
            PGPSignature pgpSignature = sigList.get(0);

            PGPPublicKey publicKey = keysMap.getKey(pgpSignature.getKeyID());

            if (!trustMap.isKeyTrustedForURI(uri, publicKey)) {
                String msg = String.format("%s=0x%X", uri, publicKey.getKeyID());
                LOG.error(String.format("Not allowed artifact %s and keyID:\n\t%s\n", uri, msg));
                return false;
            }

            pgpSignature.init(new BcPGPContentVerifierBuilderProvider(), publicKey);
            pgpSignature.update(data);

            String msgFormat = "%s PGP Signature %s\n       KeyId: 0x%X UserIds: %s";
            if (pgpSignature.verify()) {
                LOG.info(String.format(msgFormat, uri,
                        "OK", publicKey.getKeyID(), Arrays.asList(publicKey.getUserIDs())));
                if (WEAK_SIGALGS.containsKey(pgpSignature.getHashAlgorithm())) {
                    if (failWeakSignature) {
                        LOG.error("Weak signature algorithm used: "
                                + WEAK_SIGALGS.get(pgpSignature.getHashAlgorithm()));
                        return false;
                    } else {
                        LOG.warn("Weak signature algorithm used: "
                                + WEAK_SIGALGS.get(pgpSignature.getHashAlgorithm()));
                    }
                }
                return true;
            } else {
                LOG.warn(String.format(msgFormat, uri,
                        "ERROR", publicKey.getKeyID(), Arrays.asList(publicKey.getUserIDs())));
                LOG.warn(uri);
                return false;
            }
        } catch (PGPException ex) {
            throw new IOException(ex);
        }
    }

    public boolean verifyByStoredChecksum(String uri, byte[] content) {
        return trustMap.isTrustedChecksumStoredForURI(uri, content);
    }
    
    
}
