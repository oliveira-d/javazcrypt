package com.dualboot.javazcrypt;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.nio.charset.StandardCharsets;

import java.util.Scanner;

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

public class ContentManager {

    private static String[] entryFields = {"user","password","URL","TOTP"};

    public static void printBytes(byte[] decryptedBytes) {
        String decryptedContent = new String(decryptedBytes, StandardCharsets.UTF_8);
        System.out.printf("%s",decryptedContent);
    }

    public static Element createFolder(Document passwordDataBase, Element folder, String newFolderName) {
        if (!folder.getTagName().equals("dir")){
            System.err.println("Failed to create folder inside"+folder.getAttribute("name")+". "+folder.getAttribute("name")+" is not a folder.");
            return folder;
        }
        Element newFolder = passwordDataBase.createElement("dir");
        newFolder.setAttribute("name",newFolderName);
        folder.appendChild(newFolder);
        return newFolder;
    }

    public static Element createEntry(Document passwordDatabase, Element folder, String newEntryName) {
        if (!folder.getTagName().equals("dir")){
            System.err.println("Failed to create folder inside"+folder.getAttribute("name")+". "+folder.getAttribute("name")+" is not a folder.");
            return folder;
        }
        Element newEntry = passwordDatabase.createElement("entry");
        newEntry.setAttribute("name",newEntryName);
        folder.appendChild(newEntry);
        for (int i=0; i<entryFields.length; i++) {
            Element newField = passwordDatabase.createElement("field");
            newField.setAttribute("name",entryFields[i]);
            newEntry.appendChild(newField);            
        }
        return newEntry;
    }

    public static void inputText(Document passwordDataBase, Element field, String newText) {
        if (!field.getTagName().equals("field")) {
            System.err.println("Failed to write to field. "+field.getAttribute("name")+" is note an input field.");
            return;
        }
        field.appendChild(passwordDataBase.createTextNode(newText));
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
