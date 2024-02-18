package com.github.oliveiradd.javazcrypt;

import java.util.Timer;
import java.util.TimerTask;

import java.util.Scanner;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.builtins.Completers.FileNameCompleter;

import java.io.Console;

class InputHandler {

    private static Timer timer = new Timer();

    private int timeInterval;
    private Scanner scanner = null;
    private LineReader lineCompleter = null;
    private LineReader lineEditor = null;

    InputHandler(Terminal terminal,int timeInterval) {
        this.timeInterval = timeInterval*1000; // convert to milisseconds
        startTimer();
        scanner = new Scanner(System.in);
        lineCompleter = LineReaderBuilder.builder().terminal(terminal).completer(new FileNameCompleter()).build();
        lineEditor = LineReaderBuilder.builder().terminal(terminal).build();
    }

    void setInactivityLimit(int seconds) {
        this.timeInterval = seconds*1000;
        resetTimer();
    }

    private void startTimer() {
        this.timer = new Timer();
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println();
                Main.clearScreen();
                System.exit(0);
            }
        }, this.timeInterval); // run in 10k milisseconds        
    }

    private void resetTimer() {
        this.timer.cancel();
        startTimer();
    }
    
    void close() {
        this.timer.cancel();
        this.scanner.close();
    }

    String nextLine() {
        resetTimer();
        return this.scanner.nextLine();
    }

    String readPassword(String prompt) {
        resetTimer();
        return new String(System.console().readPassword(prompt));
    }
    
    String completeLine(String prompt) {
        resetTimer();
        return lineCompleter.readLine(prompt);
    }

    String editLine(String prompt, String editable) {
        resetTimer();
        return lineEditor.readLine(prompt,null,editable);
    }

}