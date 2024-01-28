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

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        String inputFilePath = "file.txt";
        String encryptedFilePath = "encrypted_file.txt";
        String decryptedFilePath = "decrypted_file.txt";
        // String password = "yourSecretPassword";

        if (!(args.length > 0)) {
            System.out.println("No arguments were provided."); return;
        }
        if (!(args.length >= 2)) {
            System.out.println("Not enough arguments provided."); return;
        }
        if (args[0].equals("--help")) return;

        Scanner scanner = new Scanner(System.in);
        System.out.printf("Enter a password: ");
        String password = scanner.nextLine();

        if (args[0].equals("-e")) { 
            try {
                byte[] encryptedBytes = CryptOps.encryptFile(args[1], password);
                ContentManager.printBytes(encryptedBytes);
                ContentManager.writeBytesToFile(args[1],encryptedBytes);
            } catch (Exception e) {
                System.out.println("Error while encrypting file");
            }
        }

        if (args[0].equals("-d")) {
            try {
                byte[] decryptedBytes = CryptOps.decryptFile(args[1], password);
                ContentManager.printBytes(decryptedBytes);
            } catch (Exception e) {
                System.out.println("Error while decrypting file");
            }
        }
    }
}
