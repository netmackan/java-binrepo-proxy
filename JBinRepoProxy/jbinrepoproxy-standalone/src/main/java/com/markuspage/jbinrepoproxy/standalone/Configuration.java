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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Parses and holds the application configuration.
 *
 * @author Markus Kilås
 */
public class Configuration {
    private static final String PROPERTY_STANDALONE_HOST = "standalone.host";
    private static final String PROPERTY_STANDALONE_PORT = "standalone.port";
    private static final String PROPERTY_TARGET_HOST = "target.host";
    private static final String PROPERTY_TARGET_PORT = "target.port";
    private static final String PROPERTY_TARGET_SCHEME = "target.scheme";
    private static final String PROPERTY_TRUST_MAP_FILE = "trust.map.file";
    private static final String PROPERTY_CACHE_KEYS_FOLDER = "cache.keys.folder";
    private static final String PROPERTY_CACHE_KEYS_SERVER = "cache.keys.server";

    private final File file;
    private final String host;
    private final int port;
    private final String targetHost;
    private final int targetPort;
    private final String targetScheme;
    private final File trustMapFile;
    private final File cacheKeysFolder;
    private final String cacheKeysServer;

    public static Configuration fromFile(File file) throws FileNotFoundException, IOException {
        // Load configuration properties
        Properties config = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            config.load(in);
        }

        // Bind address
        final String host = config.getProperty(PROPERTY_STANDALONE_HOST);
        final int port = Integer.parseInt(config.getProperty(PROPERTY_STANDALONE_PORT));

        // Target address
        final String targetHost = config.getProperty(PROPERTY_TARGET_HOST);
        final int targetPort = Integer.parseInt(config.getProperty(PROPERTY_TARGET_PORT));
        final String targetScheme = config.getProperty(PROPERTY_TARGET_SCHEME);

        // Trust map file
        final File trustMapFile;
        File f = new File(config.getProperty(PROPERTY_TRUST_MAP_FILE));
        if (f.isAbsolute()) {
            trustMapFile = f;
        } else {
            trustMapFile = new File(file.getParentFile().getAbsolutePath(), f.getPath());
        }

        // Cache keys folder
        final File cacheKeysFolder;
        f = new File(config.getProperty(PROPERTY_CACHE_KEYS_FOLDER));
        if (f.isAbsolute()) {
            cacheKeysFolder = f;
        } else {
            cacheKeysFolder = new File(file.getParentFile().getAbsolutePath(), f.getPath());
        }
        final String cacheKeysServer = config.getProperty(PROPERTY_CACHE_KEYS_SERVER);

        return new Configuration(file, host, port, targetScheme, targetHost, targetPort, trustMapFile, cacheKeysFolder, cacheKeysServer);
    }

    public Configuration(final File file, final String host, final int port, final String targetScheme, final String targetHost, final int targetPort, final File trustMapFile, final File cacheKeysFolder, final String cacheKeysServer) {
        this.file = file;
        this.host = host;
        this.port = port;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.targetScheme = targetScheme;
        this.trustMapFile = trustMapFile;
        this.cacheKeysFolder = cacheKeysFolder;
        this.cacheKeysServer = cacheKeysServer;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public String getTargetScheme() {
        return targetScheme;
    }

    public File getTrustMapFile() {
        return trustMapFile;
    }

    public File getCacheKeysFolder() {
        return cacheKeysFolder;
    }

    public String getCacheKeysServer() {
        return cacheKeysServer;
    }

    @Override
    public String toString() {
        return "Configuration{" + file + "}";
    }
    
}
