package com.dualboot.javazcrypt;

import java.io.Console;
import java.util.Scanner;

import java.util.LinkedList;
import java.util.Deque;

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
import org.w3c.dom.Text;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

// copy function
import java.io.StringWriter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;

public class Main {

    private static LinkedList<String> pathL = new LinkedList<>();
    private static Deque<String> pathQ = pathL;
    // private static Document passwordDatabase;
    private static String password;
    private static String keyFile;
    private static String inputFile;

    public static void main(String[] args) {

        if (args.length == 0) {
            System.err.println("No arguments were provided.\nDisplaying help instead:"); return;
        }

        keyFile = null;
        String operation = "open";
        inputFile = null;

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
        } else {
            Path inputFilePath = Paths.get(inputFile);
            if (Files.exists(inputFilePath)) {
                System.err.printf("File already exists. Will not overwrite %s%nExiting.%n",inputFile);
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
        password = null;
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
        Element currentElement = passwordDatabase.getDocumentElement(); // gets the root element
        // LinkedList<String> pathL = new LinkedList<>();
        // Deque<String> pathQ = pathL;
        // main interaction with database
        do {
            currentElement = mainMenu(passwordDatabase,currentElement);
            if (currentElement != null) {
                currentElement = entryMenu(passwordDatabase,currentElement);
            }
        } while (currentElement != null);
        
    }

    // private static Element entryMenu(Document passwordDatabase, Element currentElement, String input, Deque pathQ) {
    private static Element entryMenu(Document passwordDatabase,Element currentElement) {
        Scanner scanner = new Scanner(System.in);
        String input = null;
        int items;
        int intInput;
        String mode = "c";
        do {
            // display
            switch (mode) {
                case "c":
                    System.out.printf("selected mode: (c) copy%nenter (e) to enter edit mode%n");
                    break;
                case "e":
                    System.out.printf("selected mode: (e) edit%nenter (c) to enter copy mode%n");
                    break;
            }
            System.out.printf("Path: /");
            for (int i=0; i<pathQ.size(); i++) {
                System.out.printf("%s/",pathL.get(pathQ.size()-1-i));
            }
            System.out.println();
            items = ContentManager.listChildElements(currentElement);
            System.out.println("e - edit mode | c - copy mode | w - write to file | q - quit | number - select field | .. - get to parent directory");
            System.out.printf("Enter the chosen option: ");
            // switch-case
            input = scanner.nextLine();
            intInput = items; // intentionally set intInput = items so that the last line in this do-while just does not execute in case there's an exception when converting string to int
            switch (input) {
                case "c":
                    mode = "c";
                    break;
                case "e":
                    mode = "e";
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
                    break;
                case "..":
                    Node parentNode = currentElement.getParentNode();
                    if (parentNode != null && parentNode.getNodeType() == Node.ELEMENT_NODE) {
                        currentElement = (Element) parentNode;
                        pathQ.pop();
                        clearScreen(); // needed here while not in the mainMenu because of return statement below
                        return currentElement;
                    }
                    break;
                case "q":
                    break;
                default:
                    try {
                        intInput = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
            }
            clearScreen();
            if (intInput < items) {
                if (mode.equals("c")) {
                    Element field = ContentManager.getChildElement(currentElement,intInput);
                    String text = field.getTextContent();
                    // TransformerFactory transformerFactory = TransformerFactory.newInstance();
                    try {
                        // Transformer transformer = transformerFactory.newTransformer();
                        // StringWriter writer = new StringWriter();
                        // transformer.transform(new DOMSource(textNode), new StreamResult(writer));
                        // String text = writer.toString();

                        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
                        Clipboard clipboard = defaultToolkit.getSystemClipboard();
                        clipboard.setContents(new StringSelection(text), null);
                    } catch (Exception e) {
                        System.err.println("Could not copy string to clipboard.");
                        String enter = scanner.nextLine();
                    }
                }
                if (mode.equals("e")) {
                    System.out.println("Enter text to input: ");
                    String text = scanner.nextLine();
                    Text textNode = passwordDatabase.createTextNode(text);
                    // delete old node first, otherwise the statement below will just append.
                    ContentManager.deleteTextContent(ContentManager.getChildElement(currentElement,intInput));
                    ContentManager.getChildElement(currentElement,intInput).appendChild(textNode);
                }
            }
        } while (!input.equals("q"));
        return null;
    }

    private static Element mainMenu(Document passwordDatabase,Element currentElement) {
        Scanner scanner = new Scanner(System.in);
        String input = null;
        int items;
        int intInput;
        do {
            // display path
            System.out.printf("Path: /");
            for (int i=0; i<pathQ.size(); i++){
                System.out.printf("%s/",pathL.get(pathQ.size()-1-i));
            }
            System.out.println();

            //display main menu
            items = ContentManager.listChildElements(currentElement);
            System.out.println("d - create directory | e - create entry | f - edit entry field | del - delete item | w - write to file | q - quit | number - select directory or entry | .. - get to parent directory");
            System.out.printf("Enter the chosen option: ");
            // get input and make decisions
            input = scanner.nextLine();
            intInput = items; // intentionally set intInput = items so that the last line in this do-while just does not execute in case there's an exception when converting string to int
            switch (input) {
                case "d":
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
                        System.out.printf("Enter entry name: ");
                        String entryName = scanner.nextLine();
                        ContentManager.createEntry(passwordDatabase,currentElement,entryName);
                    } else {
                        System.out.printf("Cannot create entry.%n%s is not a folder.",currentElement.getAttribute("name"));
                    }
                    break;
                case "del":
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
                        pathQ.pop();
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
            clearScreen();
            if (intInput < items) {
                currentElement = ContentManager.getChildElement(currentElement,intInput);
                pathQ.push(currentElement.getAttribute("name"));
                if (currentElement.getTagName().equals("entry")){
                    return currentElement;
                }
            }
        } while (!input.equals("q"));
        return null;
    }

    private static void clearScreen() {
        // ANSI escape code to clear the screen
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
