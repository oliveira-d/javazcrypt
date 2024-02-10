package com.dualboot.javazcrypt;

import java.io.Console;
import java.util.Scanner;

import java.util.LinkedList;
import java.util.Deque;

// file checking
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// check is file is readable/writable
import java.nio.file.FileSystems;

import java.util.Base64;

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

// autocompletion
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.FileNameCompleter; // Import FileNameCompleter from correct package

// generate passwd
import java.security.SecureRandom;

// clear clipboard
import java.util.Timer;
import java.util.TimerTask;

public class Main {

    private static LinkedList<String> pathL = new LinkedList<>();
    private static Deque<String> pathQ = pathL;
    private static String password = null;
    private static String keyFile = null;
    private static String inputFile = null;
    private static String outputFile = null;
    private static Terminal terminal = getTerminal();
    private static pxmlElement clipboardElement = null; // leave it to the class so that is doesn't lose itself when switching between mainMenu() and entryMenu()
    private static boolean saved = true;
    private static Scanner scanner = new Scanner(System.in);
    private static Timer timer = null; // do not initialize timer here. For operations other than manipulating the database the timer won't be canceled and program will hang instead of quitting
    private static String message = null;

    public static void main(String[] args) {

        if (args.length == 0) {
            System.err.println("No arguments were provided.\nDisplaying help."); help(); return;
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
                    break;
                case "-d":
                case "--decrypt":
                    operation = "decrypt";
                    break;
                case "-o":
                case "--output":
                    if (i < args.length - 1) {
                        outputFile = args[++i];
                    } else {
                        System.err.println("Missing output file.");
                    }
                    break;
                case "-h":
                case "--help":
                    help();
                    return;
                default:
                    inputFile = args[i];
            }
        }

        if (inputFile == null) {
            System.err.println("No database specified.");
            System.exit(1);
        } else if (!fileExists(inputFile)) {
            operation="create";
            System.out.println("Database not found. Creating new.");
        } else if (!isRegularFile(inputFile)) {
            System.err.printf("Cannot open database. %s is not a regular file.%nExiting.%n",inputFile);
            System.exit(1);
        }

        if (outputFile == null) {
            outputFile = inputFile;
        } else if (fileExists(outputFile)) {
            System.err.println("File already exists. Will not overwrite.");
            System.exit(1);
        }

        if (keyFile != null) {
            if (!fileExists(keyFile)) {
                System.err.printf("Could not find key file %s%nExiting.%n",keyFile);
                System.exit(1);
            }
        }

        // get password
        Console console = System.console();
        char[] passwordChars = null;
        if (operation.equals("open") || operation.equals("decrypt")) {
            passwordChars = console.readPassword("Enter your password: ");
            password = new String(passwordChars);
        } else { // if (operation.equals("encrypt") || operation.equals("create"))
            String password2 = null;
            do {
                passwordChars = console.readPassword("Enter a password to encrypt the file: ");
                password = new String(passwordChars);
                passwordChars = console.readPassword("Confirm your password: ");
                password2 = new String(passwordChars);
                if (!password2.equals(password)) System.out.println("Passwords do not match. Try again.");
            } while (!password2.equals(password));
        }
        
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

        if (passwordDatabase == null) System.exit(1);

        pxmlElement currentElement = new pxmlElement(passwordDatabase);

        clearScreen();
        timer = new Timer(); // timer is only initialized here to avoid the program hanging instead of finishing execution after operations executed previously in the code 
        do {
            currentElement = mainMenu(passwordDatabase,currentElement);
            if (currentElement != null) {
                currentElement = entryMenu(passwordDatabase,currentElement);
            }
        } while (currentElement != null);
        timer.cancel();
        ContentManager.copyToClipboard(null); // clear clipboard in case the timer hasn't gotten to do so

        if(!saved) {
            System.out.printf("There are changes not saved to the file. Would you like to write to disk? (y/n): ");
            String answer = scanner.nextLine();
            answer = answer.toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) saveFile(passwordDatabase);
        }
        scanner.close();
    }

    private static boolean fileExists(String file) {
        Path filePath = Paths.get(file);
        if (Files.exists(filePath)) {
            return true;
        }
        return false;
    }

    private static boolean isRegularFile(String file) {
        Path filePath = Paths.get(file);
        if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
            return true;
        }
        return false;
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
            return null;
        }
    }

    private static Document createDatabase() {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document passwordDatabase = documentBuilder.newDocument();
            Element rootElement = passwordDatabase.createElement("dir");
            rootElement.setAttribute("name","root");
            passwordDatabase.appendChild(rootElement);
            byte[] decryptedBytes = ContentManager.convertXMLDocumentToByteArray(passwordDatabase);
            byte[] encryptedBytes = CryptOps.encryptBytes(decryptedBytes,password,keyFile);
            ContentManager.writeBytesToFile(inputFile,encryptedBytes);
            return passwordDatabase;
        } catch(Exception e) {
            System.err.println("Error creating database.");
            e.printStackTrace();
            return null;
        }
    }

    // private static Element entryMenu(Document passwordDatabase, Element currentElement, String input, Deque pathQ) {
    private static pxmlElement entryMenu(Document passwordDatabase,pxmlElement currentElement) {
        String input = null;
        int items;
        int index;
        String mode = "copy";
        do {
            if (message != null) {
                System.out.println(message);
                message = null;
                fillWidth("=");
            }
            // display
            switch (mode) {
                case "copy":
                    System.out.printf("selected mode: (c) copy%nenter (e) for edit mode%n");
                    break;
                case "edit":
                    System.out.printf("selected mode: (e) edit%nenter (c) for copy mode%n");
                    break;
            }
            fillWidth("=");
            System.out.printf("Path: /");
            for (int i=0; i<pathQ.size(); i++) {
                System.out.printf("%s/",pathL.get(pathQ.size()-1-i));
            }
            System.out.println();
            fillWidth("=");
            System.out.println();
            items = currentElement.listChildElements(true);
            System.out.println();
            fillWidth("=");
            String[] options0 = {" number - select field ","   e - edit mode   ","  c - copy mode  "};
            String[] options1 = {" g - generate password "," w - write to file "," 0 - close entry "};
            displayMenu(options0);
            displayMenu(options1);
            fillWidth("=");
            System.out.printf("Enter menu option: ");
            // switch-case
            input = scanner.nextLine();
            index = items+1; // intentionally set index > items + 1 so that the last line in this do-while just does not execute in case there's an exception when converting string to int
            switch (input) {
                case "c":
                    mode = "copy";
                    break;
                case "e":
                    mode = "edit";
                    break;
                case "w":
                    saveFile(passwordDatabase);
                    break;
                case "..":
                case "0":
                    Node parentNode = currentElement.getParentNode();
                    if (parentNode != null && parentNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element tempElement = (Element) parentNode;
                        currentElement = new pxmlElement(tempElement);
                        pathQ.pop();
                        clearScreen(); // needed here while not in the mainMenu because of return statement below
                        return currentElement;
                    }
                    break;
                case "g":
                    String[] allPasswordElements = {"qwertyuiopasdfghjklçzxcvbnm","QWERTYUIOPASDFGHJKLÇZXCVBNM","0123456789","!@#$%¨&*()_-+=`´[]{}~^;:.><","¬¹²³£¢§°®ŧ←↓→øþ´ªæßðđŋħˀĸł´ºˇ«»©„“”µ•·̣"}; // NO ALPHABETICAL ORDER, DEAL WITH IT      
                    for (int i=0; i<allPasswordElements.length; i++) {
                        System.out.println((i+1)+") "+allPasswordElements[i]);
                    }
                    System.out.printf("Enter the indexes of elements you want in your password: ");
                    input = scanner.nextLine();
                    String chosenPasswordsElements = "";
                    for (int i=0; i<allPasswordElements.length; i++) {
                        if(input.contains(String.valueOf(i+1))) {
                            chosenPasswordsElements += allPasswordElements[i];
                        }
                    }
                    System.out.printf("Enter desired password length: ");
                    input = scanner.nextLine();
                    try {
                        int length = Integer.parseInt(input);
                        String entryPassword = generatePassword(length,chosenPasswordsElements);
                        Text textNode = passwordDatabase.createTextNode(entryPassword);
                        currentElement.getChildElement(ContentManager.passwordIndex).deleteTextContent(); // passwordEntry = 1
                        currentElement.getChildElement(ContentManager.passwordIndex).appendChild(textNode);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    try {
                        index = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (index <= items && index >= 1) {
                        if (mode.equals("edit")) {
                            LineReader lineReader = LineReaderBuilder.builder().terminal(terminal).build();
                            pxmlElement field = currentElement.getChildElement(index-1);
                            String text = null;
                            if (field.getAttribute("name").equals("TOTP")) {
                                text = lineReader.readLine("Edit "+field.getAttribute("name")+" secret key: ",null,field.getTextContent());
                            } else {
                                text = lineReader.readLine("Edit "+field.getAttribute("name")+": ",null,field.getTextContent());
                            }
                            Text textNode = passwordDatabase.createTextNode(text);
                            // delete old node first, otherwise the statement below will just append.
                            field.deleteTextContent();
                            field.appendChild(textNode);
                        } else {
                            pxmlElement field = currentElement.getChildElement(index-1);
                            String text = field.getTextContent();
                            if (field.getAttribute("name").equals("TOTP")) {
                                if (field.getTextContent().length() > 0) {
                                    text = TOTP.getCode(field.getTextContent());
                                }
                            }
                            ContentManager.copyToClipboard(text);
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    ContentManager.copyToClipboard(null);
                                }
                            }, 10000); // run in 10k milisseconds
                        }
                    }
            }
            clearScreen();
        } while (!input.equals("q"));
        return null;
    }

    private static pxmlElement mainMenu(Document passwordDatabase,pxmlElement currentElement) {
        String input = null;
        int items;
        int index;
        pxmlElement fileElement = null;
        LineReader lineReader = null;
        do {
            if (message != null) {
                System.out.println(message);
                message = null;
            }
            // display clipboard
            if (clipboardElement != null) {
                fillWidth("=");
                System.out.println("On clipboard: "+clipboardElement.getAttribute("name")+" - enter mv to move it here.");
            }
            // display path
            fillWidth("=");
            System.out.printf("Path: /");
            for (int i=0; i<pathQ.size(); i++){
                System.out.printf("%s/",pathL.get(pathQ.size()-1-i));
            }
            System.out.println();
            fillWidth("=");
            System.out.println();
            //display main menu
            items = currentElement.listChildElements(false);
            System.out.println();
            fillWidth("=");
            String[] options0 = {" d - create directory ","   e - create entry   ","      r - rename      "};
            String[] options1 = {"  w - write to file   ","   .. or 0 - cd out   ","     del - delete     "};
            String[] options2 = {"       q - quit       ","  p - change password "," k - change key file  "};
            String[] options3 = {"      mv - move       ","   if - import file   ","   ef - export file   "};
            displayMenu(options0);
            displayMenu(options1);
            displayMenu(options2);
            displayMenu(options3);
            fillWidth("=");
            System.out.printf("Enter menu option: ");
            // get input and make decisions
            input = scanner.nextLine();
            index = items+1; // intentionally set index > items so that the last line in this do-while just does not execute in case there's an exception when converting string to int
            switch (input) {
                case "d":
                    System.out.printf("Enter directory name: ");
                    String folderName = scanner.nextLine();
                    currentElement.createChild(passwordDatabase,"dir",folderName);
                    saved = false;
                    break;
                case "e":
                    System.out.printf("Enter entry name: ");
                    String entryName = scanner.nextLine();
                    currentElement.createChild(passwordDatabase,"entry",entryName);
                    saved = false;
                    break;
                case "del":
                    System.out.printf("Enter index to delete: ");
                    input = scanner.nextLine();
                    try {
                        index = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (index <= items && index >= 1) {
                        currentElement.deleteItem(index-1);
                        saved = false;
                    }
                    break;
                case "q":
                    break;
                case "w":
                    saveFile(passwordDatabase);
                    break;
                case "0":
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
                case "r":
                    System.out.printf("Enter index to rename: ");
                    input = scanner.nextLine();
                    try {
                        index = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (index <= items && index >= 1) {
                        System.out.printf("Enter new name: ");
                        String name = scanner.nextLine();
                        currentElement.getChildElement(index-1).setAttribute("name",name);
                        saved = false;
                    }
                    break;
                case "p":
                    changePassword();
                    break;
                case "k":
                    lineReader = LineReaderBuilder.builder().terminal(terminal).completer(new FileNameCompleter()).build();
                    keyFile = lineReader.readLine("Enter path to key file: ");
                    while (keyFile.endsWith(" ")) {
                        // Remove the space using substring to avoid exception - this space may occur when completing with tab
                        keyFile = keyFile.substring(0, keyFile.length() - 1);
                    }
                    if (fileExists(keyFile) && isRegularFile(keyFile)) {
                        if (!Files.isReadable(FileSystems.getDefault().getPath(keyFile))) {
                            message = "Cannot read key file "+keyFile;
                            keyFile = null;
                        }
                        saved = false;
                    } else {
                        message = "Cannot find key file"+keyFile;
                        keyFile = null;
                    }
                    break;
                case "mv":
                    if (clipboardElement == null) {
                        System.out.printf("Enter index to move: ");
                        input = scanner.nextLine();
                        try {
                            index = Integer.parseInt(input);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                        if (index <= items && index >= 1) clipboardElement = currentElement.getChildElement(index-1);
                    } else {
                        currentElement.appendChild(clipboardElement);
                        clipboardElement = null;
                        saved = false;
                    }
                    break;
                case "if":
                    lineReader = LineReaderBuilder.builder().terminal(terminal).completer(new FileNameCompleter()).build();
                    String importedFile = lineReader.readLine("Enter the path for the file you wish to import to this database: ");
                    while (importedFile.endsWith(" ")) {
                        // Remove the space using substring to avoid exception - this space may occur when completing with tab
                        importedFile = importedFile.substring(0, importedFile.length() - 1);
                    }
                    if (!isRegularFile(importedFile)) {
                        message = "Cannot import file: "+importedFile+" is not a regular file.";
                        break;
                    }
                    if (!Files.isReadable(FileSystems.getDefault().getPath(importedFile))){
                        message = "Cannot import file: "+importedFile+" is not readable.";
                        break;
                    }
                    byte[] fileBytes = null; // compiler complains if i don't initialize it
                    try {
                        fileBytes = Files.readAllBytes(Paths.get(importedFile));
                    } catch (IOException e) {
                        message = "Could not open file "+importedFile;
                        break;
                    }
                    System.out.printf("Enter new name for the file: ");
                    String newFileName = scanner.nextLine();
                    String base64EncodedFile = Base64.getEncoder().encodeToString(fileBytes);
                    fileElement = currentElement.createChild(passwordDatabase,"file",newFileName);
                    fileElement.inputText(passwordDatabase,base64EncodedFile);
                    saved = false;
                    break;
                case "ef":
                    System.out.printf("Enter index of the file you want to output: ");
                    input = scanner.nextLine();
                    try {
                        index = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (index <= items && index >= 1) {
                        fileElement = currentElement.getChildElement(index-1);
                        if (!fileElement.getTagName().equals("file")) {
                            message = "Cannot complete operation: "+fileElement.getAttribute("name")+" is not a file.";
                            break;
                        }
                        String encodedBytes = fileElement.getTextContent();
                        byte[] decodedBytes = Base64.getDecoder().decode(encodedBytes);
                        lineReader = LineReaderBuilder.builder().terminal(terminal).completer(new FileNameCompleter()).build();
                        String outputDecodedFile = lineReader.readLine("Enter file to output data: ");
                        while (outputDecodedFile.endsWith(" ")) {
                            // Remove the space using substring to avoid exception
                            outputDecodedFile = outputDecodedFile.substring(0, outputDecodedFile.length() - 1);
                        }
                        try{
                            ContentManager.writeBytesToFile(outputDecodedFile,decodedBytes);
                        } catch (IOException e) {
                            message = "Could not output decoded file.";
                        }
                    }
                    break;
                default:
                    try {
                        index = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (index <= items && index >= 1) {
                        if (currentElement.getChildElement(index-1).getTagName().equals("file")) break;
                        currentElement = currentElement.getChildElement(index-1);
                        pathQ.push(currentElement.getAttribute("name"));
                        if (currentElement.getTagName().equals("entry")){
                            clearScreen();
                            return currentElement;
                        }
                    }
            }
            clearScreen();
        } while (!input.equals("q"));
        return null;
    }

    private static void saveFile(Document passwordDatabase) {
        try {
            byte[] decryptedBytes = ContentManager.convertXMLDocumentToByteArray(passwordDatabase);
            byte[] encryptedBytes = CryptOps.encryptBytes(decryptedBytes,password,keyFile);
            ContentManager.writeBytesToFile(inputFile,encryptedBytes);
            System.out.println("Content successfully written to file!");
            saved = true;
            } catch (Exception e) {
                message = "Could not write content to file.";
                e.printStackTrace();
        }
    }

    private static void displayMenu(String[] options) {
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

    private static void fillWidth(String filling) {
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

    private static String generatePassword(int length, String possibleCharacters) {
        SecureRandom random = new SecureRandom();
        StringBuilder stringBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(possibleCharacters.length());
            char randomChar = possibleCharacters.charAt(randomIndex);
            stringBuilder.append(randomChar);
        }
        return stringBuilder.toString();
    }

    private static void changePassword() {
        Console console = System.console();
        char[] oldPasswordChars = console.readPassword("Enter current password: ");
        String oldPassword = new String(oldPasswordChars);
        if (oldPassword.equals("..")) return;
        char[] newPasswordChars;
        char[] newPasswordChars2;
        String newPassword;
        String newPassword2;        
        // if (oldPassword.equals(password)) {
        do {
            newPasswordChars = console.readPassword("Enter new password: ");
            newPassword = new String(newPasswordChars);
            if (newPassword.equals("..")) return;
            newPasswordChars2 = console.readPassword("Confirm password: ");
            newPassword2 = new String(newPasswordChars2);
            if (newPassword2.equals("..")) return;
            if (!newPassword2.equals(newPassword)) System.err.println("Passwords do not match. Try again.");
        } while (!newPassword2.equals(newPassword));
        password = newPassword;
        clearScreen();
        System.out.println("Password set successfully!");
        // }
    }

    private static void help() {
        String help = """
        Usage:

        java -jar javazcrypt.jar [options] <file_path>

        Options:

        -k <keyfile_path>  Use a key file, additionally to a password.
        -d, --decrypt      Decrypt file.
        -e, --encrypt      Encrypt file.
        -o <output_file>   Specify output file when encrypting or decrypting. 
                        If no output file is specified file will be edited in place.

        If neither decryption or encryption operation is specified, 
        javazcrypt will try to open or create a password database.

        Examples:

        javar -jar javazcrypt.jar myPasswordDatabase

        java -jar javazcrypt.jar -k keyFile myPasswordDatabase

        java -jar javazcrypt.jar -d myEncryptedFile

        java -jar javazcrypt.jar -e -o encryptedOutputFile myDecryptedFile
        """;
        System.out.println(help);
    }
}
