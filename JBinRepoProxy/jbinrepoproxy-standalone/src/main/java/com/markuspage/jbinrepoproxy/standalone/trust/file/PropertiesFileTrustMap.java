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
package com.markuspage.jbinrepoproxy.standalone.trust.file;

import com.markuspage.jbinrepoproxy.standalone.trust.TrustMap;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TrustMap implementation base on a Properties file.
 *
 * @author Markus Kilås
 */
public class PropertiesFileTrustMap implements TrustMap {

    /** Logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(PropertiesFileTrustMap.class);
    
    private static final String FINGERPRINT_PREFIX = "FINGERPRINT";
    private static final String TRUSTFILE_PREFIX = "TRUSTFILE";
    private static final String DIGEST_PREFIX = "TRUSTEDDIGEST";

    private final Properties properties;

    public static PropertiesFileTrustMap fromFile(File file) throws IOException, FileNotFoundException, PGPException {
        final Properties properties = new Properties();
        try (FileInputStream fin = new FileInputStream(file)) {
            properties.load(fin);
        }

        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(TRUSTFILE_PREFIX)) {
                String artifact = key.substring(TRUSTFILE_PREFIX.length() + 1);
                final String f = properties.getProperty(key);
                LOG.info("Loading trusted keys for {} from {}", artifact, f);

                // File names are relative to the property file
                File pubFile = new File(f);
                if (!pubFile.isAbsolute()) {
                    pubFile = new File(file.getParentFile(), f);
                }

                for (PGPPublicKey publicKey : parsePublicKeys(pubFile)) {
                    String keyKey = FINGERPRINT_PREFIX + "." + Hex.toHexString(publicKey.getFingerprint()).toUpperCase(Locale.US);

                    String oldValue = properties.getProperty(keyKey);
                    String newValue = artifact;
                    if (oldValue != null) {
                        HashSet<String> values = new HashSet<>(Arrays.<String>asList(oldValue.split(",")));
                        values.add(artifact);
                        newValue = String.join(",", values);
                    }
                    properties.setProperty(keyKey, newValue);
                }
            }
        }
        return new PropertiesFileTrustMap(properties);
    }

    private PropertiesFileTrustMap(Properties properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        properties.stringPropertyNames().stream().forEach((key) -> {
            sb.append(key).append("=").append(properties.getProperty(key)).append("\n");
        });
        return sb.toString();
    }

    @Override
    public boolean isKeyTrustedForURI(String uri, PGPPublicKey publicKey) {
        LOG.info("isValidKey({}, {}) ?", uri, String.format("0x%X", publicKey.getKeyID()));
        String fingerprint = Hex.toHexString(publicKey.getFingerprint()).toUpperCase(Locale.US);
        LOG.info("fingerprint: {}", fingerprint);
        String allowedPath = properties.getProperty(FINGERPRINT_PREFIX + "." + fingerprint);

        if (allowedPath == null) {
            LOG.info("No entry for {}", fingerprint);
            return false;
        } else {
            boolean allowed = false;
            String[] paths = allowedPath.split(",");
            for (String path : paths) {
                if (uri.startsWith(path)) {
                    allowed = true;
                    break;
                }
            }
            return allowed;
        }

    }
    
    @Override
    public Collection<String> getTrustedURIs(PGPPublicKey publicKey) {
        String fingerprint = Hex.toHexString(publicKey.getFingerprint()).toUpperCase(Locale.US);
        LOG.info("fingerprint: {}", fingerprint);
        String allowedPath = properties.getProperty(FINGERPRINT_PREFIX + "." + fingerprint);
        return allowedPath == null ? Collections.emptyList() : Arrays.asList(allowedPath);
    }

    private static final String BEGIN_PUBLIC_KEY = "-----BEGIN PGP PUBLIC KEY BLOCK-----";
    private static final String END_PUBLIC_KEY = "-----END PGP PUBLIC KEY BLOCK-----";

    private static List<PGPPublicKey> parsePublicKeys(File file) throws IOException, PGPException {
        final ArrayList<PGPPublicKey> results = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.equals(BEGIN_PUBLIC_KEY)) {
                    LOG.debug("Skipping: {}", line);
                } else {
                    final StringBuilder buff = new StringBuilder();
                    buff.append(BEGIN_PUBLIC_KEY).append("\n");
                    while ((line = reader.readLine()) != null && !line.equals(END_PUBLIC_KEY)) {
                        buff.append(line).append("\n");
                    }
                    if (line == null) {
                        throw new IOException("Premature end of file, expected " + END_PUBLIC_KEY);
                    }
                    buff.append(END_PUBLIC_KEY).append("\n");

                    // Now parse the key
                    InputStream keyIn = PGPUtil.getDecoderStream(new ByteArrayInputStream(buff.toString().getBytes()));
                    PGPPublicKeyRingCollection pgpRing = new PGPPublicKeyRingCollection(keyIn, new BcKeyFingerprintCalculator());

                    Iterator<PGPPublicKeyRing> keyRings = pgpRing.getKeyRings();
                    while (keyRings.hasNext()) {
                        PGPPublicKeyRing ring = keyRings.next();
                        Iterator<PGPPublicKey> publicKeys = ring.getPublicKeys();
                        while (publicKeys.hasNext()) {
                            PGPPublicKey publicKey = publicKeys.next();
                            results.add(publicKey);
                        }
                    }
                }
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    LOG.warn("Failed to close reader", ex);
                }
            }
        }
        return results;
    }

    @Override
    public boolean isTrustedChecksumStoredForURI(String uri, byte[] data) {
        final boolean result;

        try {
            MessageDigest md;
            md = MessageDigest.getInstance("SHA-256");
            String actualValue = Hex.toHexString(md.digest(data));
            String digestValue = properties.getProperty(DIGEST_PREFIX + "." + uri);
            if (digestValue == null) {
                LOG.info("No trusted digest for this file: {}\nTRUSTEDDIGEST.{}={}", uri, uri, actualValue);
                result = false;
            } else {
                result = digestValue.equalsIgnoreCase(actualValue);
            }
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }

        return result;
    }
}
