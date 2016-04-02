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

import org.apache.http.examples.StartableReverseProxy;
import java.io.IOException;
import org.apache.http.HttpHost;

/**
 * Standalone proxy application.
 *
 * @author Markus Kilås
 */
public class Main {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: jbinrepoproxy-standalone <PORT> <TARGET HOSTNAME> [TARGET PORT]");
            System.err.println("Example: jbinreporoxy-standalone 8888 repo1.example.com 80");
            System.exit(1);
        }
        final int port = Integer.parseInt(args[0]);
        final String targetHost = args[1];
        int targetPort = 80;
        if (args.length > 1) {
            targetPort = Integer.parseInt(args[2]);
        }
        System.out.println("Will proxy to " + targetHost + ":" + targetPort);
        new StartableReverseProxy().start(port, new HttpHost(targetHost, targetPort));
    }

}
