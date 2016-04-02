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
package org.apache.http.examples;

import java.io.IOException;
import org.apache.http.HttpHost;

/**
 * Extends the proxy implementation so that it can be started.
 *
 * @author Markus Kilås
 */
public class StartableReverseProxy extends ElementalReverseProxy {
    
    public void start(final int port, final HttpHost target) throws IOException {
        final Thread t = new StartableReverseProxy.RequestListenerThread(8888, target);
        t.setDaemon(false);
        t.start();
    }
    
}
