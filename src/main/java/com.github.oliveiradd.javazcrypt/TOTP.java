package com.github.oliveiradd.javazcrypt;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import org.apache.commons.codec.binary.Base32;

class TOTP {
    
    // private static final int TOTP_INTERVAL_SECONDS = 30; // Time step in seconds
    // private static final int TOTP_DIGITS = 6;

    static String getCode(String secretKeyBase32, String algorithm, String totpIntervalStr, String numberOfDigitsStr) {
        int totpInterval = 30;
        try {
            totpInterval = Integer.parseInt(totpIntervalStr);
        } catch (NumberFormatException e) {
        }

        int numberOfDigits = 6;
        try {
            numberOfDigits = Integer.parseInt(numberOfDigitsStr);
        } catch (NumberFormatException e) {
        }

        long counter = getCurrentTimeStep(totpInterval);
        byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();

        byte[] secretKey = decodeBase32(secretKeyBase32);

        byte[] hmac = generateHMACSHA1(secretKey, counterBytes, algorithm);
        int offset = hmac[hmac.length - 1] & 0x0F;
        int binary = ((hmac[offset] & 0x7F) << 24) | ((hmac[offset + 1] & 0xFF) << 16) | ((hmac[offset + 2] & 0xFF) << 8) | (hmac[offset + 3] & 0xFF);

        int totp = binary % (int) Math.pow(10, numberOfDigits);
        return String.format("%0" + numberOfDigits + "d", totp);
    }

    private static byte[] generateHMACSHA1(byte[] key, byte[] data, String algorithm) {
        try {
            Mac hmacSHA1 = Mac.getInstance(algorithm);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "RAW");
            hmacSHA1.init(secretKeySpec);
            return hmacSHA1.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error generating HMAC-SHA1", e);
        }
    }

    private static long getCurrentTimeStep(int totpInterval) {
        return Instant.now().getEpochSecond() / totpInterval;
    }

    private static byte[] decodeBase32(String encodedString) {
        Base32 base32 = new Base32();
        byte[] decodedBytes = base32.decode(encodedString);
        return decodedBytes;
    }
}