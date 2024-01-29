package com.dualboot.javazcrypt;

import java.util.Scanner;

public class Main {

    public static boolean inPlace = false;
    
    public static void main(String[] args) {

        if (args.length == 0) {
            System.err.println("No arguments were provided.\nDisplaying help instead:"); return;
        }

        if (args.length == 1) {
            System.err.println("Not enough arguments were provided.\nDisplaying help instead:"); return;
        }
        
        String keyFile = null;
        String operation = null;
        String inputFile = null;
        String outputFile = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-e":
                case "-d":
                case "--encrypt":
                case "--decrypt":
                    operation = args[i];
                    break;
                case "-k":
                    if (i < args.length - 1) {
                        keyFile = args[++i]; // ++i - Move to the next argument - keyfile path won't serve in switch case anyway
                    } else {
                        System.out.println("Missing key file.");
                        System.exit(1);
                    }
                    break;
                case "-o":
                case "--output":
                    if (i < args.length -1) {
                        outputFile = args[++i];
                    } else {
                        System.err.println("Missing output file.");
                        System.exit(1);
                    }
                    break;
                case "-i":
                case "--in-place":
                    inPlace = true;
                    break;
                default:
                    inputFile = args[i];
            }
        }

        Scanner scanner = new Scanner(System.in);
        System.out.printf("Enter a password: ");
        String password = scanner.nextLine();

        if (operation.equals("-e") || operation.equals("--encrypt")) { 
            try {
                byte[] encryptedBytes = CryptOps.encryptFile(inputFile, password, keyFile);
                ContentManager.printBytes(encryptedBytes);
                if (inPlace == true) ContentManager.writeBytesToFile(inputFile,encryptedBytes);
                if (outputFile != null) ContentManager.writeBytesToFile(outputFile,encryptedBytes);
            } catch (Exception e) {
                System.out.println("Error while encrypting file");
            }
        } else if (operation.equals("-d") || operation.equals("--decrypt")) {
            try {
                byte[] decryptedBytes = CryptOps.decryptFile(inputFile, password,keyFile);
                ContentManager.printBytes(decryptedBytes);
                if (inPlace == true) ContentManager.writeBytesToFile(inputFile,decryptedBytes);
                if (outputFile != null) ContentManager.writeBytesToFile(outputFile,decryptedBytes);
            } catch (Exception e) {
                System.out.println("Error while decrypting file");
                e.printStackTrace();
            }
        } else {
            System.err.println("Operation not recognized.");
        }
    }
}
