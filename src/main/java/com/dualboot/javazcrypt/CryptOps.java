package com.dualboot.javazcrypt;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.spec.KeySpec;

import java.nio.charset.StandardCharsets;

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

    public static void printBytes(byte[] decryptedBytes) {
        String decryptedContent = new String(decryptedBytes, StandardCharsets.UTF_8);
        System.out.printf("%s",decryptedContent);
    } 

    private static Key generateKey(String password) throws Exception {
    // Use PBKDF2 with SHA-256
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), password.getBytes(), 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
    }

    public static void writeBytesToFile(String outputFilePath, byte[] outputBytes) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {
            outputStream.write(outputBytes);
        }
    }
}
