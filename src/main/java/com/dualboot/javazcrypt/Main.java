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

public class Main {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    public static void encryptFile(String inputFilePath, String outputFilePath, String password) {
        try {
            Key key = generateKey(password);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] inputFileBytes = Files.readAllBytes(Paths.get(inputFilePath));
            byte[] encryptedBytes = cipher.doFinal(inputFileBytes);

            try (FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {
                outputStream.write(encryptedBytes);
            }

            System.out.println("File encrypted successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void decryptFile(String inputFilePath, String outputFilePath, String password) {
        try {
            Key key = generateKey(password);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] inputFileBytes = Files.readAllBytes(Paths.get(inputFilePath));
            byte[] decryptedBytes = cipher.doFinal(inputFileBytes);

            try (FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {
                outputStream.write(decryptedBytes);
            }

            System.out.println("File decrypted successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // private static Key generateKey(String password) throws Exception {
    //     return new SecretKeySpec(password.getBytes(), ALGORITHM);
        
    // }

    private static Key generateKey(String password) throws Exception {
    // Use PBKDF2 with SHA-256
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), password.getBytes(), 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
    }

    public static void main(String[] args) {
        String inputFilePath = "file.txt";
        String encryptedFilePath = "encrypted_file.txt";
        String decryptedFilePath = "decrypted_file.txt";
        String password = "yourSecretPassword";

        if (!(args.length > 0)) {
            System.out.println("No arguments were provided."); return;
        }
        if (!(args.length >= 2)) {
            System.out.println("Not enough arguments provived."); return;
        }
        if (args[0].equals("--help")) return;

        if (args[0].equals("-e")) encryptFile(args[1], encryptedFilePath, password);
        if (args[0].equals("-d")) decryptFile(args[1], decryptedFilePath, password);
    }
}
