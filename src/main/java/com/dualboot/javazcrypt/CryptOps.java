package com.dualboot.javazcrypt;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.spec.KeySpec;

import java.security.Key;

import java.nio.file.Files;
import java.nio.file.Paths;

public class CryptOps {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    public static byte[] encryptFile(String inputFilePath, String password) throws Exception {
        
        Key key = generateKey(password);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] inputFileBytes = Files.readAllBytes(Paths.get(inputFilePath));
        byte[] encryptedBytes = cipher.doFinal(inputFileBytes);

        System.out.println("File encrypted successfully.");
        return encryptedBytes;
    }

    public static byte[] decryptFile(String inputFilePath, String password) throws Exception {
        
        Key key = generateKey(password);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] inputFileBytes = Files.readAllBytes(Paths.get(inputFilePath));
        byte[] decryptedBytes = cipher.doFinal(inputFileBytes);

        System.out.println("File decrypted successfully.");
        return decryptedBytes;
    }

    private static Key generateKey(String password) throws Exception {
    // Use PBKDF2 with SHA-256
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), password.toUpperCase().getBytes(), 65536, 256); // 256 bits is the maximum key size hehe
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
    }

}
