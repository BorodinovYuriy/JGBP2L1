package com.geekbrains.sep22.geekcloudclient;

public enum Command {
    SEND_FILE("sendFileToServer"),
    GET_FILE("getFilesFromTheServer"),
    UPDATE_FILE("updateFileServer"),

    SEND_FILE_TO_USER("sendFileToUser");


    String command;
    Command(String command){
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

}