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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.util.encoders.Hex;

/**
 *
 * @author Markus Kilås
 */
public class KeysMap {

    private static final String FINGERPRINT_PREFIX = "FINGERPRINT";
    private static final String TRUSTFILE_PREFIX = "TRUSTFILE";
    
    private final Properties properties;

    public KeysMap(Properties properties) {
        this.properties = properties;
    }
    
    public void load() throws IOException, FileNotFoundException, PGPException {
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(TRUSTFILE_PREFIX)) {
                String artifact = key.substring(TRUSTFILE_PREFIX.length() + 1);
                final String file = properties.getProperty(key);
                System.out.println("Loading trusted keys for " + artifact + " from " + file);
                
                for (PGPPublicKey publicKey : parsePublicKeys(new File(file))) {
                    //System.out.println("# Id: " + String.format("0x%X", publicKey.getKeyID()));
                    //System.out.println(FINGERPRINT_PREFIX + "." + Hex.toHexString(publicKey.getFingerprint()) + "=" + artifact);
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
    }
    
    public void print() {
        for (String key : properties.stringPropertyNames()) {
            System.out.println(key + "=" + properties.getProperty(key));
        }
    }

    public boolean isValidKey(String uri, PGPPublicKey publicKey) {
        System.out.println("isValidKey(" + uri + ", " + String.format("0x%X", publicKey.getKeyID()) + " ?");
        String fingerprint = Hex.toHexString(publicKey.getFingerprint()).toUpperCase(Locale.US);
        System.out.println("fingerprint: " + fingerprint);
        String allowedPath = properties.getProperty(FINGERPRINT_PREFIX + "." + fingerprint);
        
        if (allowedPath == null) {
            System.err.println("No entry for " + fingerprint);
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
    
    private static final String BEGIN_PUBLIC_KEY = "-----BEGIN PGP PUBLIC KEY BLOCK-----";
    private static final String END_PUBLIC_KEY = "-----END PGP PUBLIC KEY BLOCK-----";

    private List<PGPPublicKey> parsePublicKeys(File file) throws IOException, PGPException {
        final ArrayList<PGPPublicKey> results = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.equals(BEGIN_PUBLIC_KEY)) {
                    System.out.println("Skipping: " + line);
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
                    Logger.getLogger(KeysMap.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return results;
    }
}
