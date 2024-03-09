package com.github.oliveiradd.javazcrypt;

// keep track of path inside "xml filesystem"
import java.util.LinkedList;
import java.util.Deque;

// file checking
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// encoding to store files in xml
import java.util.Base64;

// xml document
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

// generate passwd
import java.security.SecureRandom;

// clear clipboard
import java.util.Timer;
import java.util.TimerTask;

class Main {

    private static LinkedList<String> pathL = new LinkedList<>();
    private static Deque<String> pathQ = pathL;
    private static String password = null;
    private static String keyFile = null;
    private static String inputFile = null;
    private static String outputFile = null;
    private static Terminal terminal = getTerminal();
    private static CustomElement clipboardElement = null; // leave it to the class so that is doesn't lose itself when switching between mainMenu() and secondaryMenu()
    private static boolean saved = true;
    private static Document passwordDatabase = null;

    private static int inactivityTimeLimit = 120; // seconds
    private static InputHandler inputHandler = new InputHandler(terminal,inactivityTimeLimit);

    private static Timer timer = null; // do not initialize timer here. For operations other than manipulating the database the timer won't be canceled and program will hang instead of quitting
    private static int clipboardTimeLimit = 10; // default time interval to clear clipboard (seconds) - may be changed in the open database function
    
    static String message = null;
    private static boolean exitProgram = false;

    // maximum imported file size (in mb)
    private static long maxFileSize = 30;

    public static void main(String[] args) {

        String operation = "open";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
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
                // general arguments
                case "-k":
                    if (i < args.length - 1) {
                        keyFile = args[++i]; // ++i - Move to the next argument - keyfile path won't serve in switch case anyway
                    } else {
                        System.out.println("Missing key file.");
                        System.exit(1);
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
            if (operation.equals("open")) {
                inputFile = inputHandler.completeLine("No database specified. Enter the path for an existing database or create a new one: ");
            } else { // operation is encrypt or decrypt
                System.err.println("No input file specified.");
                System.exit(1);
            }
        }

        if (!Files.exists(Paths.get(inputFile))) {
            if (operation.equals("open")) {
                operation="create";
                System.out.println("Database not found. Creating new.");
            } else {
                System.err.printf("Cannot open '%s': file does not exist.%n",inputFile);
                System.exit(1);
            }
        } else if (!Files.isReadable(Paths.get(inputFile))) {
            System.err.printf("Cannot open '%s': no read permission.%n",inputFile);
            System.exit(1);
        } else if (!Files.isRegularFile(Paths.get(inputFile))) {
            System.err.printf("Cannot open '%s': not a regular file.%n",inputFile);
            System.exit(1);
        }

        if (operation.equals("encrypt") || operation.equals("decrypt")) {
            if (outputFile == null) {
                outputFile = inputFile;
            } else if (!Files.exists(Paths.get(outputFile))) {
                System.err.printf("'%s' already exists. Will not overwrite.%n",outputFile);
                System.exit(1);
            }
        }

        if (keyFile != null) {
            if (!Files.exists(Paths.get(keyFile))) {
                System.err.printf("Cannot open '%s': file does not exist.%n",keyFile);
                System.exit(1);
            } else if (!Files.isReadable(Paths.get(keyFile))) {
                System.err.printf("Cannot open '%s': no read permission.%n",keyFile);
                System.exit(1);
            } else if (!Files.isRegularFile(Paths.get(keyFile))) {
                System.err.printf("Cannot open '%s': not a regular file.%n",keyFile);
                System.exit(1);
            }
        }

        // get password
        password = getPassword(operation);
        
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

        CustomElement currentElement = new CustomElement(passwordDatabase);

        clearScreen();
        timer = new Timer(); // timer is only initialized here to avoid the program hanging instead of finishing execution after operations executed previously in the code 
        do {
            currentElement = mainMenu(currentElement);
            if (currentElement != null) {
                currentElement = secondaryMenu(currentElement);
            }
        } while (currentElement != null);
        timer.cancel();
        ContentManager.copyToClipboard(null); // clear clipboard in case the timer hasn't gotten to do so

        if(!saved) {
            System.out.printf("Database has been modified. Would you like to save the changes? (y/n): ");
            String answer = inputHandler.nextLine();
            answer = answer.toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) saveFile();
        }
        inputHandler.close();
    }

    private static Document createDatabase() {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document passwordDatabase = documentBuilder.newDocument();
            Element rootElement = passwordDatabase.createElement("dir");
            rootElement.setAttribute("name","root");
            rootElement.setAttribute("clipboardTimeLimit",String.valueOf(clipboardTimeLimit)); //set time interval in seconds
            rootElement.setAttribute("inactivityTimeLimit",String.valueOf(inactivityTimeLimit));
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

    private static Document openDatabase() {
        try {
            byte[] decryptedBytes = CryptOps.decryptFile(inputFile, password,keyFile);
            Document passwordDatabase = ContentManager.convertByteArrayToXMLDocument(decryptedBytes);
            System.out.println("Database opened successfuly.");
            try {
                clipboardTimeLimit = Integer.parseInt(passwordDatabase.getDocumentElement().getAttribute("clipboardTimeLimit")); // convert to milisseconds
            } catch (NumberFormatException e) {
                message = "Could not get clipboard time limit from settings. Using default.";
            }
            try {
                inactivityTimeLimit = Integer.parseInt(passwordDatabase.getDocumentElement().getAttribute("inactivityTimeLimit"));
            } catch (NumberFormatException e) {
                message = "Could not get inactivity time limit from settings. Using default.";
            }
            inputHandler.setInactivityLimit(inactivityTimeLimit);
            return passwordDatabase;
        } catch (Exception e) {
            System.out.println("Error opening database");
            return null;
        }
    }

    private static CustomElement mainMenu(CustomElement currentElement) {
        String input = null;
        int items;
        int index;
        CustomElement fileElement = null;
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
            items = currentElement.displayChildElements(false,false);
            System.out.println();
            fillWidth("=");
            String[] options0 = {" d - add directory ","   e - add entry  "," c - add credit card "};
            String[] options1 = {"  r - rename item  ","  mv - move item  ","     del - delete    "};
            String[] options2 = {"  if - import file "," ef - export file ","     w - write out   "};
            String[] options3 = {"  .. or 0 - cd out ","   s - settings   ","       q - quit      "};
            displayMenu(options0);
            displayMenu(options1);
            displayMenu(options2);
            displayMenu(options3);
            fillWidth("=");
            System.out.printf("Enter menu option: ");
            // get input and make decisions
            input = inputHandler.nextLine();
            index = items+1; // intentionally set index > items so that the last line in this do-while just does not execute in case there's an exception when converting string to int
            switch (input) {
                case "d":
                    System.out.printf("Enter directory name: ");
                    String folderName = inputHandler.nextLine();
                    currentElement.createChild(passwordDatabase,"dir",folderName);
                    saved = false;
                    autoSave();
                    break;
                case "e":
                    System.out.printf("Enter entry name: ");
                    String entryName = inputHandler.nextLine();
                    currentElement.createChild(passwordDatabase,"entry",entryName);
                    saved = false;
                    autoSave();
                    break;
                case "c":
                    System.out.printf("Enter credit card name: ");
                    String cardName = inputHandler.nextLine();
                    currentElement.createChild(passwordDatabase,"card",cardName);
                    saved = false;
                    autoSave();
                    break;
                case "del":
                    System.out.printf("Enter index to delete: ");
                    input = inputHandler.nextLine();
                    try {
                        index = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (index <= items && index >= 1) {
                        currentElement.deleteItem(index-1);
                        saved = false;
                        autoSave();
                    }
                    break;
                case "q":
                    exitProgram = true;
                    break;
                case "w":
                    saveFile();
                    break;
                case "0":
                case "..":
                    if (!currentElement.getTagName().equals("dir") || !currentElement.getAttribute("name").equals("root")) {
                        Node parentNode = currentElement.getParentNode();
                        if (parentNode != null && parentNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element tempElement = (Element) parentNode;
                            currentElement = new CustomElement(tempElement);
                            pathQ.pop();
                        }
                    }
                    break;
                case "r":
                    System.out.printf("Enter index to rename: ");
                    input = inputHandler.nextLine();
                    try {
                        index = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (index <= items && index >= 1) {
                        String name = inputHandler.editLine("Enter new name: ",currentElement.getChildElement(index-1).getAttribute("name"));
                        currentElement.getChildElement(index-1).setAttribute("name",name);
                        currentElement.insertElement(currentElement.getChildElement(index-1)); // this is done to move it and keep alphabetical order
                        saved = false;
                        autoSave();
                    }
                    break;
                case "s":
                    clearScreen();
                    settingsMenu();
                    break;
                case "mv":
                    if (clipboardElement == null) {
                        System.out.printf("Enter index to move: ");
                        input = inputHandler.nextLine();
                        try {
                            index = Integer.parseInt(input);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                        if (index <= items && index >= 1) clipboardElement = currentElement.getChildElement(index-1);
                    } else {
                        currentElement.insertElement(clipboardElement);
                        clipboardElement = null;
                        saved = false;
                        autoSave();
                    }
                    break;
                case "if":
                    String importedFile = inputHandler.completeLine("Enter the path for the file you wish to import to this database: ");
                    if (!Files.exists(Paths.get(importedFile))) {
                        message = "Cannot import '"+importedFile+"': file does not exist.";
                    } else if (!Files.isReadable(Paths.get(importedFile))) {
                        message = "Cannot import '"+importedFile+"': no read permission.";
                    } else if (!Files.isRegularFile(Paths.get(importedFile))) {
                        message = "Cannot import '"+importedFile+"': not a regular file.";
                    } else {
                        byte[] fileBytes = null; // compiler complains if i don't initialize it
                        try {
                            if (Files.size(Paths.get(importedFile)) > maxFileSize*1024*1024) {
                                message = "Cannot import '"+importedFile+"': file is larger the maximum supported size of "+maxFileSize+" MiB";
                                break;
                            }
                            fileBytes = Files.readAllBytes(Paths.get(importedFile));
                        } catch (IOException e) {
                            message = "Could not open file "+importedFile;
                            break;
                        }
                        System.out.printf("Enter new name for the file: ");
                        String newFileName = inputHandler.nextLine();
                        String base64EncodedFile = Base64.getEncoder().encodeToString(fileBytes);
                        fileElement = currentElement.createChild(passwordDatabase,"file",newFileName);
                        fileElement.inputText(passwordDatabase,base64EncodedFile);
                        saved = false;
                        autoSave();
                    }
                    break;
                case "ef":
                    System.out.printf("Enter index of the file you want to output: ");
                    input = inputHandler.nextLine();
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
                        String outputDecodedFile = inputHandler.completeLine("Enter file to output data: ");
                        if (Files.exists(Paths.get(outputDecodedFile))) {
                            message = "Could not output decoded file: file exists, will not overwrite";
                            break;
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
                        if (currentElement.getTagName().equals("entry") || currentElement.getTagName().equals("card")){
                            clearScreen();
                            return currentElement;
                        }
                    }
            }
            clearScreen();
        } while (!exitProgram);
        return null;
    }

    private static CustomElement secondaryMenu(CustomElement currentElement) {
        String input = null;
        int items;
        int index;
        String mode = "copy";
        boolean hideSensitiveValues = true;
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
            items = currentElement.displayChildElements(true,hideSensitiveValues);
            System.out.println();
            fillWidth("=");
            String[] options0 = {" number - select field "," g - generate password "," v - toggle visibility "};
            String[] options1 = {" 0 or .. - close entry ","    s - setup TOTP     ","       q - quit        "};
            if (currentElement.getTagName().equals("card")) {
                options0[1] = "";
                options1[1] = ""; // case of card = no password
            }
            displayMenu(options0);
            displayMenu(options1);
            fillWidth("=");
            System.out.printf("Enter menu option: ");
            // switch-case
            input = inputHandler.nextLine();
            index = items+1; // intentionally set index > items + 1 so that the last line in this do-while just does not execute in case there's an exception when converting string to int
            switch (input) {
                case "c":
                    mode = "copy";
                    break;
                case "e":
                    mode = "edit";
                    break;
                case "w":
                    saveFile();
                    break;
                case "..":
                case "0":
                    Node parentNode = currentElement.getParentNode();
                    if (parentNode != null && parentNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element tempElement = (Element) parentNode;
                        currentElement = new CustomElement(tempElement);
                        pathQ.pop();
                        clearScreen(); // needed here while not in the mainMenu because of return statement below
                        return currentElement;
                    }
                    break;
                case "g":
                    if (currentElement.getTagName().equals("card")) break;
                    String[] allPasswordElements = {"qwertyuiopasdfghjklçzxcvbnm","QWERTYUIOPASDFGHJKLÇZXCVBNM","0123456789","!@#$%¨&*()_-+=`´[]{}~^;:.><","¬¹²³£¢§°®ŧ←↓→øþ´ªæßðđŋħˀĸł´ºˇ«»©„“”µ•·̣"}; // NO ALPHABETICAL ORDER, DEAL WITH IT      
                    for (int i=0; i<allPasswordElements.length; i++) {
                        System.out.println((i+1)+") "+allPasswordElements[i]);
                    }
                    System.out.printf("Enter the indexes of elements you want in your password: ");
                    input = inputHandler.nextLine();
                    String chosenPasswordsElements = "";
                    for (int i=0; i<allPasswordElements.length; i++) {
                        if(input.contains(String.valueOf(i+1))) {
                            chosenPasswordsElements += allPasswordElements[i];
                        }
                    }
                    System.out.printf("Enter desired password length: ");
                    input = inputHandler.nextLine();
                    try {
                        int length = Integer.parseInt(input);
                        String entryPassword = generatePassword(length,chosenPasswordsElements);
                        Text textNode = passwordDatabase.createTextNode(entryPassword);
                        currentElement.getChildElement(CustomElement.passwordIndex).deleteTextContent(); // passwordEntry = 1
                        currentElement.getChildElement(CustomElement.passwordIndex).appendChild(textNode);
                        saved = false;
                        autoSave();
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    break;
                case "t":
                    CustomElement totpField = currentElement.getChildElement(CustomElement.TOTPIndex);
                    setupTOTP(totpField);
                    break;
                case "q":
                    exitProgram = true;
                case "v":
                    hideSensitiveValues = !hideSensitiveValues;
                default:
                    try {
                        index = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (index <= items && index >= 1) {
                        if (mode.equals("edit")) {
                            CustomElement field = currentElement.getChildElement(index-1);
                            String text = null;
                            if (field.getAttribute("name").equals("TOTP")) {
                                // set defaults
                                field.setAttribute("totpInterval","30");
                                field.setAttribute("algorithm","HmacSHA1");
                                field.setAttribute("numberOfDigits","6");
                                text = inputHandler.editLine("Edit "+field.getAttribute("name")+" secret key: ",field.getTextContent());
                            } else {
                                text = inputHandler.editLine("Edit "+field.getAttribute("name")+": ",field.getTextContent());
                            }
                            Text textNode = passwordDatabase.createTextNode(text);
                            // delete old node first, otherwise the statement below will just append.
                            field.deleteTextContent();
                            field.appendChild(textNode);
                            saved = false;
                            autoSave();
                        } else {
                            CustomElement field = currentElement.getChildElement(index-1);
                            String text = field.getTextContent();
                            if (field.getAttribute("name").equals("TOTP")) {
                                if (field.getTextContent().length() > 0) {
                                    text = TOTP.getCode(field.getTextContent(),field.getAttribute("algorithm"),field.getAttribute("totpInterval"),field.getAttribute("numberOfDigits"));
                                }
                            }
                            ContentManager.copyToClipboard(text);
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    ContentManager.copyToClipboard(null);
                                }
                            }, clipboardTimeLimit*1000); // run in 10k milisseconds
                        }
                    }
            }
            clearScreen();
        } while (!exitProgram);
        return null;
    }

    private static void setupTOTP(CustomElement totpField) {
        String algorithm;
        do {
            algorithm = inputHandler.editLine("Enter algorithm (HmacSHA1, HmacSHA256, HmacSHA512): ",totpField.getAttribute("algorithm"));
            if (!algorithm.equals("HmacSHA1") && !algorithm.equals("HmacSHA256") && !algorithm.equals("HmacSHA512")) System.err.println("Invalid algorithm. Note that the input is case sensitive.");
        } while (!algorithm.equals("HmacSHA1") && !algorithm.equals("HmacSHA256") && !algorithm.equals("HmacSHA512"));
        totpField.setAttribute("algorithm",algorithm);
        // set time interval
        String totpIntervalStr;
        int totpInterval = 0;
        do {
            totpIntervalStr = inputHandler.editLine("Enter TOTP time interval (seconds): ",totpField.getAttribute("totpInterval"));
            try {
                totpInterval = Integer.parseInt(totpIntervalStr);
                if (totpInterval == 0) System.err.println("Interval cannot be zero.");
            } catch (NumberFormatException e) {
                System.err.println("Not a integer number. Try again.");
            }
        } while (totpInterval == 0);
        totpField.setAttribute("totpInterval",totpIntervalStr);
        //set number of digits
        String numberOfDigitsStr;
        int numberOfDigits = 0;
        do {
            numberOfDigitsStr = inputHandler.editLine("Enter TOTP number of digits: ",totpField.getAttribute("numberOfDigits"));
            try {
                numberOfDigits = Integer.parseInt(numberOfDigitsStr);
                if (numberOfDigits == 0) System.err.println("Number of digits cannot be zero.");
            } catch (NumberFormatException e) {
                System.err.println("Not a integer number. Try again.");
            }
        } while (numberOfDigits == 0);
        totpField.setAttribute("numberOfDigits",numberOfDigitsStr);
    }

    private static void settingsMenu() {
        fillWidth("=");
        System.out.println();
        System.out.println("(p) change password");
        System.out.println("(k) change key file");
        System.out.println("(t) change clipboard time limit");
        System.out.println("(i) change inactivity time limit");
        System.out.println();
        fillWidth("=");
        System.out.printf("Enter an option: ");
        String input = inputHandler.nextLine();
        switch (input) {
            case "p":
                changePassword();
                break;
            case "k":
                String newKeyFile = inputHandler.completeLine("Enter path to new key file: ");
                if (newKeyFile.equals("")) {
                    if (password.equals("")) {
                        message = "Key file cannot be null when password is empty.";
                    } else {
                        keyFile = null;
                    }
                } else if (!Files.exists(Paths.get(newKeyFile))) {
                    message = "Cannot open '"+newKeyFile+"': files does not exist.";
                } else if (!Files.isReadable(Paths.get(newKeyFile))) {
                    message = "Cannot open '"+newKeyFile+"': no read permission.";
                } else if (!Files.isRegularFile(Paths.get(newKeyFile))) {
                    message = "Cannot open '"+newKeyFile+"': not a regular file.";
                } else {
                    keyFile = newKeyFile;
                    saved = false; // keyFile updated in memory
                    autoSave();
                }
                break;
            case "t":
                String clipboardTimeLimitStr = inputHandler.editLine("Enter clipboard time limit (seconds): ",passwordDatabase.getDocumentElement().getAttribute("clipboardTimeLimit"));
                try {
                    int clipboardTimeLimit = Integer.parseInt(clipboardTimeLimitStr);
                    clipboardTimeLimitStr = String.valueOf(clipboardTimeLimit);
                    passwordDatabase.getDocumentElement().setAttribute("clipboardTimeLimit",clipboardTimeLimitStr);
                    saved = false;
                    autoSave();
                } catch (NumberFormatException e) {
                    message = "Timer was not updated. Input was not a number.";
                }
                break;
            case "i":
                String inactivityTimeLimitStr = inputHandler.editLine("Enter inactivity time limit (seconds): ",passwordDatabase.getDocumentElement().getAttribute("inactivityTimeLimit"));
                try {
                    int inactivityTimeLimit = Integer.parseInt(inactivityTimeLimitStr);
                    inactivityTimeLimitStr = String.valueOf(inactivityTimeLimit);
                    passwordDatabase.getDocumentElement().setAttribute("inactivityTimeLimit",inactivityTimeLimitStr);
                    saved = false;
                    autoSave();
                    inputHandler.setInactivityLimit(inactivityTimeLimit);
                } catch (NumberFormatException e) {
                    message = "Timer was not updated. Input was not a number.";
                }
                break;
        }
    }

    static void saveFile() {
        if (saved == true) return;
        try {
            byte[] decryptedBytes = ContentManager.convertXMLDocumentToByteArray(passwordDatabase);
            byte[] encryptedBytes = CryptOps.encryptBytes(decryptedBytes,password,keyFile);
            ContentManager.writeBytesToFile(inputFile,encryptedBytes);
            // System.out.println("Content successfully written to file!");
            saved = true;
            } catch (Exception e) {
                message = "Could not write content to file.";
                e.printStackTrace();
        }
    }

    private static void autoSave() {
        Thread thread = new Thread(() -> {
            saveFile();
        });
        thread.start();
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

    private static String getPassword(String operation) {
        if (operation.equals("open") || operation.equals("decrypt")) {
            password = inputHandler.readPassword("Enter your password: ");
        } else { // if (operation.equals("encrypt") || operation.equals("create"))
            String password2 = null;
            do {
                password = inputHandler.readPassword("Enter a password to encrypt the file: ");
                password2 = inputHandler.readPassword("Confirm your password: ");
                if (!password2.equals(password)) System.out.println("Passwords do not match. Try again.");
                if (password2.equals("") && keyFile == null) System.out.println("Password cannot be empty when no key file is being used. Try again.");
            } while (!password2.equals(password) || (password2.equals("") && keyFile == null));
        }
        return password;
    }

    private static void changePassword() {
        password = getPassword("encrypt");
        saved = false;
        autoSave();
        clearScreen();
        System.out.println("Password set successfully!");
        // }
    }

    private static Terminal getTerminal() {
        try {
            Terminal terminal = TerminalBuilder.builder().build();
            return terminal;
        } catch (Exception e) {
            return null;
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

    static void clearScreen() {
        // ANSI escape code to clear the screen
        System.out.print("\033[H\033[2J");
        System.out.flush();
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
