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
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

// display menu
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class Main {

    private static LinkedList<String> pathL = new LinkedList<>();
    private static Deque<String> pathQ = pathL;
    private static String password = null;
    private static String keyFile = null;
    private static String inputFile = null;
    private static String outputFile = null;

    public static void main(String[] args) {

        if (args.length == 0) {
            System.err.println("No arguments were provided.\nDisplaying help instead:"); return;
        }

        String operation = "open";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                // arguments for the password manager
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
                    break;
                // file encryption utility arguments
                case "-e":
                case "--encrypt":
                    operation = "encrypt";
                    if (i < args.length - 1) {
                        inputFile = args[++i];
                    } else {
                        System.err.println("Missing input file.");
                    }
                    break;
                case "-d":
                case "--decrypt":
                    operation = "decrypt";
                    if (i < args.length - 1) {
                        inputFile = args[++i];
                    } else {
                        System.err.println("Missing input file.");
                    }
                    break;
                case "-o":
                case "--output":
                    if (i < args.length - 1) {
                        outputFile = args[++i];
                    } else {
                        System.err.println("Missing output file.");
                    }
                    break;
                default:
                    inputFile = args[i];
            }
        }

        if (inputFile == null) {
            System.err.println("No database specified.");
            System.exit(1);
        } else if (operation.equals("create")) {
            Path inputFilePath = Paths.get(inputFile);
            if (Files.exists(inputFilePath)) {
                System.err.printf("File already exists. Will not overwrite %s%nExiting.%n",inputFile);
                System.exit(1);
            }
        } else {
            Path inputFilePath = Paths.get(inputFile);
            if (!Files.exists(inputFilePath)) {
                operation="create";
                System.out.println("Database not found. Creating new.");
            } else if (!Files.isRegularFile(inputFilePath)) {
                System.err.printf("Could not find database. %s is not a regular file.%nExiting.%n",inputFile);
                System.exit(1);
            }
        }

        if (outputFile == null) outputFile = inputFile;

        if (keyFile != null) {
            Path keyFilePath = Paths.get(keyFile);
            if (!Files.exists(keyFilePath) || !Files.isRegularFile(keyFilePath)) {
                System.err.printf("Could not find key file %s%nExiting.%n",keyFile);
                System.exit(1);
            }
        }

        Console console = System.console();
        char[] passwordChars = null;
        if (operation.equals("open") || operation.equals("decrypt")) {
            passwordChars = console.readPassword("Enter your password: ");
            password = new String(passwordChars);
        } else if (operation.equals("encrypt")) { // operation is encrypt
            passwordChars = console.readPassword("Enter a password to encrypt the file: ");
            password = new String(passwordChars);
        } // else operation.equals("create"), which has it's own prompt inside a do-while

        Document passwordDatabase = null;
        switch (operation) {
            case "open":
                passwordDatabase = openDatabase();
                break;
            case "create":
                passwordDatabase = createDatabase();
                break;
            case "encrypt":
                try {
                    byte[] encryptedBytes = CryptOps.encryptFile(inputFile, password, keyFile);
                    ContentManager.writeBytesToFile(outputFile,encryptedBytes);
                } catch (Exception e) {
                    System.err.println("Could not encrypt file.");
                }
                return;
            case "decrypt":
                try {
                    byte[] decryptedBytes = CryptOps.decryptFile(inputFile, password, keyFile);
                    ContentManager.writeBytesToFile(outputFile,decryptedBytes);
                } catch (Exception e) {
                    System.err.println("Could not decrypt file.");
                }
                return;
        }

        pxmlElement currentElement = new pxmlElement(passwordDatabase);
        Terminal terminal = getTerminal();

        clearScreen(); 
        do {
            currentElement = mainMenu(passwordDatabase,currentElement,terminal);
            if (currentElement != null) {
                currentElement = entryMenu(passwordDatabase,currentElement,terminal);
            }
        } while (currentElement != null);
        
    }

    private static Document openDatabase() {
        try {
            byte[] decryptedBytes = CryptOps.decryptFile(inputFile, password,keyFile);
            Document passwordDatabase = ContentManager.convertByteArrayToXMLDocument(decryptedBytes);
            System.out.println("Database opened successfuly.");
            // ContentManager.printBytes(decryptedBytes);
            return passwordDatabase;
        } catch (Exception e) {
            System.out.println("Error opening database");
            System.exit(1);
            return null;
        }
    }

    private static Document createDatabase() {
        Console console = System.console();
        char[] passwordChars = null;
        String password2 = null;

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
            byte[] encryptedBytes = CryptOps.encryptBytes(decryptedBytes,password,keyFile);
            ContentManager.writeBytesToFile(inputFile,encryptedBytes);
            return passwordDatabase;
        } catch(Exception e) {
            System.err.println("Error creating database.");
            e.printStackTrace();
            System.exit(1);
            return null;
        }

    }
    // private static Element entryMenu(Document passwordDatabase, Element currentElement, String input, Deque pathQ) {
    private static pxmlElement entryMenu(Document passwordDatabase,pxmlElement currentElement, Terminal terminal) {
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
            fillWidth("=",terminal);
            System.out.printf("Path: /");
            for (int i=0; i<pathQ.size(); i++) {
                System.out.printf("%s/",pathL.get(pathQ.size()-1-i));
            }
            System.out.println();
            fillWidth("=",terminal);
            System.out.println();
            items = currentElement.listChildElements(true);
            System.out.println();
            fillWidth("=",terminal);
            String[] options = {" e - edit mode "," c - copy mode ", " w - write to file "," number - select field "," .. - get to parent directory "," q - quit "};
            displayMenu(options,terminal);
            fillWidth("=",terminal);
            System.out.printf("Enter the chosen option: ");
            // switch-case
            input = scanner.nextLine();
            intInput = items+1; // intentionally set intInput = items + 1 so that the last line in this do-while just does not execute in case there's an exception when converting string to int
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
                        Element tempElement = (Element) parentNode;
                        currentElement = new pxmlElement(tempElement);
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
            if (intInput <= items) {
                if (mode.equals("e")) {
                    System.out.println("Enter text to input: ");
                    String text = scanner.nextLine();
                    Text textNode = passwordDatabase.createTextNode(text);
                    // delete old node first, otherwise the statement below will just append.
                    currentElement.getChildElement(intInput-1).deleteTextContent();
                    currentElement.getChildElement(intInput-1).appendChild(textNode);
                    clearScreen();
                } else {
                    pxmlElement field = currentElement.getChildElement(intInput-1);                    
                    String text = field.getTextContent();
                    if (field.getAttribute("name").equals("TOTP")) {
                        if (field.getTextContent().length() > 0) {
                            text = TOTP.getCode(field.getTextContent());
                        }
                    }
                    ContentManager.copyToClipboard(text);
                }
            }
        } while (!input.equals("q"));
        return null;
    }

    private static pxmlElement mainMenu(Document passwordDatabase,pxmlElement currentElement, Terminal terminal) {
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
            fillWidth("=",terminal);
            System.out.println();
            //display main menu
            items = currentElement.listChildElements(false);
            System.out.println();
            fillWidth("=",terminal);
            String[] options0 = {" d - create directory "," e - create entry "," del - delete item "};
            String[] options1 = {" w - write to file "," number - open item "," .. - get to parent dir "};
            displayMenu(options0,terminal);
            displayMenu(options1,terminal);
            fillWidth("=",terminal);
            System.out.printf("Enter the chosen option: ");
            // get input and make decisions
            input = scanner.nextLine();
            intInput = items+1; // intentionally set intInput = items so that the last line in this do-while just does not execute in case there's an exception when converting string to int
            switch (input) {
                case "d":
                    if (currentElement.getTagName().equals("dir")) {
                        System.out.printf("Enter directory name: ");
                        String folderName = scanner.nextLine();
                        currentElement.createFolder(passwordDatabase,folderName);
                    } else {
                        System.out.printf("Cannot create directory.%n%s is not a folder.",currentElement.getAttribute("name"));
                    }
                    break;
                case "e":
                    if (currentElement.getTagName().equals("dir")) {
                        System.out.printf("Enter entry name: ");
                        String entryName = scanner.nextLine();
                        currentElement.createEntry(passwordDatabase,entryName);
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
                            currentElement.deleteItem(intIndex);
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
                        Element tempElement = (Element) parentNode;
                        currentElement = new pxmlElement(tempElement);
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
            if (intInput <= items) {
                currentElement = currentElement.getChildElement(intInput-1);
                pathQ.push(currentElement.getAttribute("name"));
                if (currentElement.getTagName().equals("entry")){
                    return currentElement;
                }
            }
        } while (!input.equals("q"));
        return null;
    }

    private static void displayMenu(String[] options, Terminal terminal) {
        int terminalWidth;
        if (terminal == null) terminalWidth = 0;
        terminalWidth = terminal.getWidth();
        int menuLength = 0;
        for (int i=0; i<options.length; i++) {
            menuLength += options[i].length();
        }
        int spacing = 0;
        if (terminalWidth > menuLength) {
            spacing = (terminalWidth-menuLength)/(options.length+1);
        }
        for (int i=0; i<options.length; i++) {
            for (int j=0; j<spacing; j++) {
                    System.out.printf(" ");
            }
            System.out.printf("%s",options[i]);
        }
        System.out.printf("%n");
    }

    private static void fillWidth(String filling, Terminal terminal) {
        if (terminal == null) return;
        int repeat = terminal.getWidth()/filling.length();
        for (int i=0; i<repeat; i++) {
            System.out.printf(filling);
        }
        System.out.printf("%n");
    }

    private static Terminal getTerminal() {
        try {
            Terminal terminal = TerminalBuilder.builder().build();
            return terminal;
        } catch (Exception e) {
            return null;
        }
    }

    private static void clearScreen() {
        // ANSI escape code to clear the screen
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
