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
package com.markuspage.jbinrepoproxy.standalone.transport.spi;

/**
 * Handler where the logic of the policy enforcement can be placed.
 *
 * The provided TransportClient can be used to fetch the requested file and/or
 * any other file before deciding if the requested file should be returned or
 * not.
 *
 * @author Markus Kilås
 */
public interface TransportHandler {
    TransportResult handleRequest(String uri, TransportRequest request, TransportClient client);
}
