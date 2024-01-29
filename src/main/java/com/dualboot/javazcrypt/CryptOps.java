package com.dualboot.javazcrypt;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.spec.KeySpec;

import java.security.Key;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CryptOps {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    public static byte[] encryptFile(String inputFile, String password, String keyFile) throws Exception {
        
        Path inputFilePath = Paths.get(inputFile);
        if (!Files.exists(inputFilePath) || !Files.isRegularFile(inputFilePath)) {
            System.err.println("Could not find input file "+inputFile);
            System.exit(1);
        }

        Key key = generateKey(password,keyFile);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] inputFileBytes = Files.readAllBytes(Paths.get(inputFile));
        byte[] encryptedBytes = cipher.doFinal(inputFileBytes);

        System.out.println("File encrypted successfully.");
        return encryptedBytes;
    }

    public static byte[] decryptFile(String inputFile, String password, String keyFile) throws Exception {
        
        Path inputFilePath = Paths.get(inputFile);
        if (!Files.exists(inputFilePath) || !Files.isRegularFile(inputFilePath)) {
            System.err.println("Could not find input file "+inputFile);
            System.exit(1);
        }

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
        byte[] passwordSalt = password.toUpperCase().getBytes();
        if (keyFile != null) {
            
            Path keyFilePath = Paths.get(keyFile);
            if (Files.exists(keyFilePath) && Files.isRegularFile(keyFilePath)) {
                passwordSalt = Files.readAllBytes(keyFilePath);
            } else {
                System.err.printf("Could not find key file %s%nExiting program.%n",keyFile);
                System.exit(1);
            }
        }
        KeySpec spec = new PBEKeySpec(password.toCharArray(), passwordSalt, 65536, 256); // 256 bits is the maximum key size hehe
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
    }

}
