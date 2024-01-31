package com.dualboot.javazcrypt;

import java.io.Console;
import java.util.Scanner;

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
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

public class PasswordManager {
    
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

        Console console = System.console();
        char[] passwordChars = null;
        String password = null;
        String password2 = null;

        if (operation.equals("create")) {
            try {
                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                Document passwordDatabase = documentBuilder.newDocument();
                // PasswordDatabase passwordDataBase = new PasswordDatabase(documentBuilder.newDocument());
                Element rootElement = passwordDatabase.createElement("root");
                passwordDatabase.appendChild(rootElement);
                Element element = passwordDatabase.createElement("element");
                element.appendChild(passwordDatabase.createTextNode("Value"));
                rootElement.appendChild(element);
                do {
                    passwordChars = console.readPassword("Enter a password to encrypt the file: ");
                    password = new String(passwordChars);
                    passwordChars = console.readPassword("Confirm your password: ");
                    password2 = new String(passwordChars);
                    if (!password2.equals(password)) System.out.println("Passwords do not match. Try again.");
                } while (!password2.equals(password));
                byte[] decryptedBytes = convertXMLDocumentToByteArray(passwordDatabase);
                byte[] encryptedBytes = CryptOps.encryptBytes(decryptedBytes,password,keyFile);
                ContentManager.writeBytesToFile(inputFile,encryptedBytes);
            } catch(Exception e) {
                System.err.println("Error creating database.");
                System.exit(1);
            }
            
        } else {
            passwordChars = console.readPassword("Enter your password: ");
            password = new String(passwordChars);            
        }

        try {
            byte[] decryptedBytes = CryptOps.decryptFile(inputFile, password,keyFile);
            Document passwordDatabase = convertByteArrayToXMLDocument(decryptedBytes);
            System.out.println("Database opened successfuly.");
            // ContentManager.printBytes(decryptedBytes);
        } catch (Exception e) {
            System.out.println("Error opening database");
        }
        // menu here
        String input;
        do {
            System.out.println("ls -l ");
            System.out.println("-");
            // List folders and items here
            System.out.println("-");
            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter the chosen option: ");
            input = scanner.nextLine();

        } while (!input.equals("e"));

        // if (operation.equals("encrypt")) {

        //     // double check password
        //     char[] passwordChars2 = console.readPassword("Confirm your password: ");
        //     String password2 = new String(passwordChars);
        //     if (!password2.equals(password)) {
        //         System.err.println("Passwords don't match. Aborting operation.");
        //         System.exit(1);
        //     }

        //     try {
        //         byte[] encryptedBytes = CryptOps.encryptFile(inputFile, password, keyFile);
        //         if (inPlace == true) ContentManager.writeBytesToFile(inputFile,encryptedBytes);
        //         if (outputFile != null) ContentManager.writeBytesToFile(outputFile,encryptedBytes);
        //     } catch (Exception e) {
        //         System.out.println("Error while encrypting file");
        //     }

        // } else if (operation.equals("decrypt")) {
            
            
            
        // } else {
        //     System.err.println("Operation not recognized.");
        // }
    }

    private static byte[] convertXMLDocumentToByteArray(Document xmlDocument) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        // Transform the XML document into a byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(xmlDocument), new StreamResult(outputStream));

        return outputStream.toByteArray();
    }

    private static Document convertByteArrayToXMLDocument(byte[] byteArray) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArray);

        return builder.parse(inputStream);
    }
}
