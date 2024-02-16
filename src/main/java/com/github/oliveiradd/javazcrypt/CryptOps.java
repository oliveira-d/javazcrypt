package com.github.oliveiradd.javazcrypt;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.spec.KeySpec;

import java.security.Key;

import java.nio.file.Files;
import java.nio.file.Paths;

class CryptOps {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    static byte[] encryptFile(String inputFile, String password, String keyFile) throws Exception {
        
        Key key = generateKey(password,keyFile);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] inputFileBytes = Files.readAllBytes(Paths.get(inputFile));
        byte[] encryptedBytes = cipher.doFinal(inputFileBytes);

        System.out.println("File encrypted successfully.");
        return encryptedBytes;
    }

    static byte[] encryptBytes(byte[] inputFileBytes, String password, String keyFile) throws Exception {
        Key key = generateKey(password,keyFile);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(inputFileBytes);
        return encryptedBytes;
    }

    static byte[] decryptFile(String inputFile, String password, String keyFile) throws Exception {
        Key key = generateKey(password,keyFile);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] inputFileBytes = Files.readAllBytes(Paths.get(inputFile));
        byte[] decryptedBytes = cipher.doFinal(inputFileBytes);

        System.out.println("File decrypted successfully.");
        return decryptedBytes;
    }

    private static Key generateKey(String password, String keyFile) throws Exception {
    // Use PBKDF2 with SHA-256
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        byte[] passwordSalt = null;
        if (keyFile == null) {
            passwordSalt = password.toUpperCase().getBytes();
        } else {
            passwordSalt = Files.readAllBytes(Paths.get(keyFile));
        }
        KeySpec spec = new PBEKeySpec(password.toCharArray(), passwordSalt, 1048576, 256); // 256 bits is the maximum key size hehe
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
    }

}
