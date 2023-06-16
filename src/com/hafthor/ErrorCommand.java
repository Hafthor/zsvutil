package com.hafthor;

public class ErrorCommand extends Command {
    public ErrorCommand(String msg) {
        super(new String[]{});
        errorMessage = msg;
    }
}