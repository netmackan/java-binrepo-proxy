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

import com.markuspage.jbinrepoproxy.standalone.transport.spi.TransactionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Markus Kilås
 */
public class TransactionLogger {
    /** Logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(TransactionLogger.class);
    
    public void log(TransactionInfo t) {
        final StringBuilder sb = new StringBuilder();
        sb.append("\n>>>\n");
        sb.append("   ").append(t.getRequestMethod()).append(" ").append(t.getUri()).append(" ").append(t.getProtocol()).append("\n");
        
        if (t.getException() != null) {
            sb.append("   ").append(t.getException().getLocalizedMessage());
        }
        sb.append("   ").append(t.getResponseCode());
        
        sb.append("\n<<<");
        
        
        
        LOG.error(sb.toString());
    }
}
