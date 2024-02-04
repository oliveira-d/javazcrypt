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
import org.w3c.dom.Text;
import org.w3c.dom.NodeList;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

// copy function
import java.io.StringWriter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;

public class pxmlElement {

    private Element element;
    private static String[] entryFields = {"user","password","URL","TOTP","notes"};
    // private static Document passwordDatabase; 

    public pxmlElement(Document passwordDatabase) {
        this.element = passwordDatabase.getDocumentElement();
    }

    public pxmlElement(Element element) {
        this.element = element;
    }


    // methods to list stuff

    public int listChildElements(boolean showValue) {
        // System.out.println("Child elements of " + parentElement.getTagName() + ":");
        Element folder = this.element;
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
                System.out.printf("(%s) (%d) %s",elementType,i+1,childElement.getAttribute("name"));
                if (showValue) {
                    switch (childElement.getAttribute("name")) {
                        case "password":
                            System.out.printf(": ");
                            for (int j=0; j<childElement.getTextContent().length(); j++) {
                                System.out.printf("*");
                            }
                            break;
                        case "TOTP":
                            if (childElement.getTextContent().length() > 0) {
                                String totp = TOTP.getCode(childElement.getTextContent());
                                System.out.printf(": %s",totp);
                            } else {
                                System.out.printf(":");
                            }
                            break;
                        default:
                            System.out.printf(": %s",childElement.getTextContent());                      
                    }
                }
                System.out.printf("%n");
                // System.out.printf("%s - %d) %s",elementType,i,childElement.getAttribute("name"));
            }
        }
        return childNodes.getLength();
    }
    
    public pxmlElement getChildElement(int index) {
        Element folder = this.element;
            // System.out.println("Child elements of " + parentElement.getTagName() + ":");
        NodeList childNodes = folder.getChildNodes();
        Node node = childNodes.item(index);
        Element childElement = null;
        if (node.getNodeType() == Node.ELEMENT_NODE) childElement = (Element) node;
        return new pxmlElement(childElement);
    }

    public pxmlElement createFolder(Document passwordDataBase, String newFolderName) {
        Element folder = this.element;
        if (!folder.getTagName().equals("dir")){
            System.err.println("Failed to create folder inside"+folder.getAttribute("name")+". "+folder.getAttribute("name")+" is not a folder.");
            return new pxmlElement(folder);
        }
        Element newFolder = passwordDataBase.createElement("dir");
        newFolder.setAttribute("name",newFolderName);
        folder.appendChild(newFolder);
        return new pxmlElement(newFolder);
    }

    public pxmlElement createEntry(Document passwordDatabase, String newEntryName) {
        Element folder = this.element;
        if (!folder.getTagName().equals("dir")){
            System.err.println("Failed to create folder inside"+folder.getAttribute("name")+". "+folder.getAttribute("name")+" is not a folder.");
            return new pxmlElement(folder);
        }
        Element newEntry = passwordDatabase.createElement("entry");
        newEntry.setAttribute("name",newEntryName);
        folder.appendChild(newEntry);
        for (int i=0; i<entryFields.length; i++) {
            Element newField = passwordDatabase.createElement("field");
            newField.setAttribute("name",entryFields[i]);
            newEntry.appendChild(newField);            
        }
        return new pxmlElement(newEntry);
    }

    public void deleteItem(int index) {
        Element folder = this.element;
        if (!folder.getTagName().equals("dir")){
            System.err.println("Failed to create item inside"+folder.getAttribute("name")+". "+folder.getAttribute("name")+" is not a folder.");
        }
        NodeList childNodes = folder.getChildNodes();
        Node childNode = childNodes.item(index);
        Element childElement = null;
        if (childNode.getNodeType() == Node.ELEMENT_NODE) childElement = (Element) childNode;
        folder.removeChild(childElement);
    }


    public void inputText(Document passwordDataBase, String newText) {
        Element field = this.element;
        if (!field.getTagName().equals("field")) {
            System.err.println("Failed to write to field. "+field.getAttribute("name")+" is note an input field.");
            return;
        }
        field.appendChild(passwordDataBase.createTextNode(newText));
    }

    public void deleteTextContent() {
        Element field = this.element;
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

    public void setAttribute(String key,String value) {
        this.element.setAttribute(key,value);
    }

    public Node getParentNode() {
        return this.element.getParentNode();
    }

    public void appendChild(Text textNode) {
        this.element.appendChild(textNode);
    }

    public String getTextContent() {
        return this.element.getTextContent();
    }

    public String getTagName() {
        return this.element.getTagName();
    }

    public String getAttribute(String key) {
        return this.element.getAttribute(key);
    }

    
}