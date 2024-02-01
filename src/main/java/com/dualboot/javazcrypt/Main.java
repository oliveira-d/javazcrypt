package com.dualboot.javazcrypt;

import java.io.Console;
import java.util.Scanner;

import java.util.LinkedList;
import java.util.Queue;

// file checking
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// dont know which ones are needed
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

public class Main {

    public static void main(String[] args) {

        if (args.length == 0) {
            System.err.println("No arguments were provided.\nDisplaying help instead:"); return;
        }

        String keyFile = null;
        String operation = "open";
        String inputFile = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-k":
                    if (i < args.length - 1) {
                        keyFile = args[++i]; // ++i - Move to the next argument - keyfile path won't serve in switch case anyway
                    } else {
                        System.out.println("Missing key file.");
                        System.exit(1);
                    }
                    break;
                case "-c":
                case "--create":
                    operation = "create";
                    if (i < args.length -1) {
                        inputFile = args[++i];
                    } else {
                        System.err.println("Missing database name.");
                    }
                default:
                    inputFile = args[i];
            }
        }

        if (inputFile == null) {
            System.err.println("No database specified.");
            System.exit(1);
        } else if (!operation.equals("create")) {
            Path inputFilePath = Paths.get(inputFile);
            if (!Files.exists(inputFilePath) || !Files.isRegularFile(inputFilePath)) {
                System.err.printf("Could not find database %s%n Exiting.%n",inputFile);
                System.exit(1);
            }
        }

        if (keyFile != null) {
            Path keyFilePath = Paths.get(keyFile);
            if (!Files.exists(keyFilePath) || !Files.isRegularFile(keyFilePath)) {
                System.err.printf("Could not find key file %s%nExiting.%n",keyFile);
                System.exit(1);
            }
        }

        Console console = System.console();
        char[] passwordChars = null;
        String password = null;
        String password2 = null;

        if (operation.equals("create")) {
            try {
                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                Document passwordDatabase = documentBuilder.newDocument();
                Element rootElement = passwordDatabase.createElement("dir");
                rootElement.setAttribute("name","root");
                passwordDatabase.appendChild(rootElement);
                do {
                    passwordChars = console.readPassword("Enter a password to encrypt the file: ");
                    password = new String(passwordChars);
                    passwordChars = console.readPassword("Confirm your password: ");
                    password2 = new String(passwordChars);
                    if (!password2.equals(password)) System.out.println("Passwords do not match. Try again.");
                } while (!password2.equals(password));
                byte[] decryptedBytes = ContentManager.convertXMLDocumentToByteArray(passwordDatabase);
                ContentManager.writeBytesToFile(inputFile+"decrypted",decryptedBytes);
                byte[] encryptedBytes = CryptOps.encryptBytes(decryptedBytes,password,keyFile);
                ContentManager.writeBytesToFile(inputFile,encryptedBytes);
            } catch(Exception e) {
                System.err.println("Error creating database.");
                e.printStackTrace();
                System.exit(1);
            }

        } else {
            passwordChars = console.readPassword("Enter your password: ");
            password = new String(passwordChars);
        }

        Document passwordDatabase = null; // initialize so that I can use in the menu
        try {
            byte[] decryptedBytes = CryptOps.decryptFile(inputFile, password,keyFile);
            passwordDatabase = ContentManager.convertByteArrayToXMLDocument(decryptedBytes);
            System.out.println("Database opened successfuly.");
            // ContentManager.printBytes(decryptedBytes);
        } catch (Exception e) {
            System.out.println("Error opening database");
            System.exit(1);
        }
        clearScreen();
        // menu here
        String input;
        Element currentElement = passwordDatabase.getDocumentElement(); // gets the root element
        Scanner scanner = new Scanner(System.in);
        LinkedList<String> pathL = new LinkedList<>();
        Queue<String> pathQ = pathL;

        // main interaction with database
        do {
            // display path
            System.out.printf("Path: /");
            for (int i=0; i<pathQ.size(); i++){
                System.out.printf("%s/",pathL.get(i));
            }
            System.out.println();

            //display main menu
            int items = ContentManager.listChildElements(currentElement);
            System.out.println("c - create directory | e - create entry | f - edit entry field | d - delete item | w - write to file | q - quit | number - select directory or entry | .. - cd ..");
            System.out.printf("Enter the chosen option: ");
            // get input and make decisions
            input = scanner.nextLine();
            int intInput = items; // intentionally set intInput = items so that the last line in this do-while just does not execute in case there's an exception when converting string to int
            switch (input) {
                case "c":
                    if (currentElement.getTagName().equals("dir")) {
                        System.out.printf("Enter directory name: ");
                        String folderName = scanner.nextLine();
                        ContentManager.createFolder(passwordDatabase,currentElement,folderName);
                    } else {
                        System.out.printf("Cannot create directory.%n%s is not a folder.",currentElement.getAttribute("name"));
                    }
                    break;
                case "e":
                    if (currentElement.getTagName().equals("dir")) {
                        System.out.printf("Enter directory name: ");
                        String entryName = scanner.nextLine();
                        ContentManager.createEntry(passwordDatabase,currentElement,entryName);
                    } else {
                        System.out.printf("Cannot create entry.%n%s is not a folder.",currentElement.getAttribute("name"));
                    }
                    break;
                case "d":
                    if (currentElement.getTagName().equals("dir")) {
                        System.out.printf("Enter index to delete: ");
                        String index = scanner.nextLine();
                        try {
                            int intIndex = Integer.parseInt(index);
                            ContentManager.deleteItem(passwordDatabase,currentElement,intIndex);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.printf("Cannot delete item.%n%s is not a folder.",currentElement.getAttribute("name"));
                    }
                    break;
                case "q":
                    break;
                case "..":
                    if (!currentElement.getTagName().equals("dir") || !currentElement.getAttribute("name").equals("root")) {
                        Node parentNode = currentElement.getParentNode();
                        if (parentNode != null && parentNode.getNodeType() == Node.ELEMENT_NODE) {
                        currentElement = (Element) parentNode;
                        pathQ.remove();
                        }
                    }
                    break;
                case "w":
                    try {
                    byte[] decryptedBytes = ContentManager.convertXMLDocumentToByteArray(passwordDatabase);
                    byte[] encryptedBytes = CryptOps.encryptBytes(decryptedBytes,password,keyFile);
                    ContentManager.writeBytesToFile(inputFile,encryptedBytes);
                    System.out.println("Content successfully written to file!");
                    } catch (Exception e) {
                        System.err.println("Could not write content to file.");
                        e.printStackTrace();
                    }
                default:
                    try {
                        intInput = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
            }
            if (intInput < items) {
                currentElement = ContentManager.getChildElement(currentElement,intInput);
                pathQ.add(currentElement.getAttribute("name"));
            }
            clearScreen();
        } while (!input.equals("q"));
    }

    private static void clearScreen() {
        // ANSI escape code to clear the screen
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
