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

public class ContentManager {

    public static void printBytes(byte[] decryptedBytes) {
        String decryptedContent = new String(decryptedBytes, StandardCharsets.UTF_8);
        System.out.printf("%s",decryptedContent);
    } 

    public static void writeBytesToFile(String outputFilePath, byte[] outputBytes) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {
            outputStream.write(outputBytes);
        }
    }
}
