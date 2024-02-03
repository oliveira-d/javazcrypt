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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

// copy function
import java.io.StringWriter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;

public class ContentManager {

    private static String[] entryFields = {"user","password","URL","TOTP","notes"};

    public static void writeBytesToFile(String outputFile, byte[] outputBytes) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            outputStream.write(outputBytes);
        }
    }

    // methods to list stuff

    public static int listChildElements(Element folder) {
        // System.out.println("Child elements of " + parentElement.getTagName() + ":");
        NodeList childNodes = folder.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) node;
                String elementType = null;
                // if (childElement.getTagName().equals("dir")) {
                //     elementType = "dir";
                // } else if (childElement.getTagName().equals("entry")) {
                //     element
                // }
                switch (childElement.getTagName()) {
                    case "dir":
                        elementType = "dir";
                        break;
                    case "entry":
                        elementType = "entry";
                        break;
                    case "field":
                        elementType = "field";
                }
                System.out.println("("+elementType+") ("+i+") "+childElement.getAttribute("name"));
                // System.out.printf("%s - %d) %s",elementType,i,childElement.getAttribute("name"));
            }
        }
        return childNodes.getLength();
    }
    
    public static Element getChildElement(Element folder, int index) {
            // System.out.println("Child elements of " + parentElement.getTagName() + ":");
        NodeList childNodes = folder.getChildNodes();
        Node node = childNodes.item(index);
        Element childElement = null;
        if (node.getNodeType() == Node.ELEMENT_NODE) childElement = (Element) node;
        return childElement;
    }

    //methods to manage XML file in memory

    public static void printBytes(byte[] decryptedBytes) {
        String decryptedContent = new String(decryptedBytes, StandardCharsets.UTF_8);
        System.out.printf("%s",decryptedContent);
    }

    public static byte[] convertXMLDocumentToByteArray(Document xmlDocument) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        // Transform the XML document into a byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(xmlDocument), new StreamResult(outputStream));

        return outputStream.toByteArray();
    }

    public static Document convertByteArrayToXMLDocument(byte[] byteArray) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArray);

        return builder.parse(inputStream);
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

    public static void deleteItem(Document document, Element folder, int index) {
        if (!folder.getTagName().equals("dir")){
            System.err.println("Failed to create item inside"+folder.getAttribute("name")+". "+folder.getAttribute("name")+" is not a folder.");
        }
        NodeList childNodes = folder.getChildNodes();
        Node childNode = childNodes.item(index);
        Element childElement = null;
        if (childNode.getNodeType() == Node.ELEMENT_NODE) childElement = (Element) childNode;
        folder.removeChild(childElement);
    }


    public static void inputText(Document passwordDataBase, Element field, String newText) {
        if (!field.getTagName().equals("field")) {
            System.err.println("Failed to write to field. "+field.getAttribute("name")+" is note an input field.");
            return;
        }
        field.appendChild(passwordDataBase.createTextNode(newText));
    }

    public static void deleteTextContent(Element field) {
        // Get the list of child nodes
        Node child = field.getFirstChild();

        // Iterate through child nodes and remove text nodes
        while (child != null) {
            if (child.getNodeType() == Node.TEXT_NODE) {
                field.removeChild(child);
            }
            child = child.getNextSibling();
        }
    }

    public static void copyToClipboard(String text) {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        Clipboard clipboard = defaultToolkit.getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
    }
}
