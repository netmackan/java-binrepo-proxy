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

import org.bouncycastle.openpgp.PGPPublicKey;

/**
 * Holder for verification data and the result.
 *
 * @author Markus Kilås
 */
public class SignatureVerificationData {

    private final SignatureResult result;
    
    private final PGPPublicKey publicKey;
    private final Exception exception;

    
    public SignatureVerificationData(SignatureResult result) {
        this.result = result;
        this.publicKey = null;
        this.exception = null;
    }

    public SignatureVerificationData(PGPPublicKey publicKey, SignatureResult result) {
        this.result = result;
        this.publicKey = publicKey;
        this.exception = null;
    }

    public SignatureVerificationData(SignatureResult result, Exception exception) {
        this.result = result;
        this.publicKey = null;
        this.exception = exception;
    }

    public SignatureVerificationData(PGPPublicKey publicKey, SignatureResult result, Exception exception) {
        this.result = result;
        this.publicKey = publicKey;
        this.exception = exception;
    }
    
    public boolean isTrusted() {
        return result == SignatureResult.TRUSTED;
    }

    public SignatureResult getResult() {
        return result;
    }

    public PGPPublicKey getPublicKey() {
        return publicKey;
    }

    public Exception getException() {
        return exception;
    }

}
