package com.dualboot.javazcrypt;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.nio.charset.StandardCharsets;

import java.util.Scanner;

public class ContentManager {

    public static void printBytes(byte[] decryptedBytes) {
        String decryptedContent = new String(decryptedBytes, StandardCharsets.UTF_8);
        System.out.printf("%s",decryptedContent);
    }

    public static void writeBytesToFile(String outputFile, byte[] outputBytes) throws IOException {
        
        Path outputFilePath = Paths.get(outputFile);
        if (Files.exists(outputFilePath) && !Main.inPlace) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("File already exists. Would you like to overwrite it? (y/n)");
            String overwrite = scanner.nextLine();
            overwrite.toLowerCase();
            if (!overwrite.equals("y") && !overwrite.equals("yes")) {
                System.out.println("Operation aborted.");
                System.exit(1);
            }

        }
        
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            outputStream.write(outputBytes);
        }
    }
}
