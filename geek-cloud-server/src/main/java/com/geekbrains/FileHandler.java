package com.geekbrains;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileHandler implements Runnable {
    //можно реализовать класс перечислений команд!!

    private static final String SERVER_DIR = "server_files";

    private static final Integer BATCH_SIZE = 1024;

    private final Socket socket;

    private final DataInputStream dis;

    private final DataOutputStream dos;
    private byte[] batch;

    List<String> serverFiles;

    //---------------------------------------------------------------
    private void sendUpdateServerList(List<String> serverFiles){
        try{
            dos.writeUTF(Command.UPDATE_FILE.getCommand());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < serverFiles.size(); i++) {
                if(i == (serverFiles.size()-1)){
                    sb.append(serverFiles.get(i));
                }else{
                    sb.append(serverFiles.get(i)+"#");
                }
            }
            dos.writeUTF(sb.toString());
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    private  List<String> getServerFilesList(String directory) {
        File dir = new File(directory);
        if (dir.isDirectory()) {
            String[] list = dir.list();
            if (list != null) {
                List<String> serverFiles = new ArrayList<>(Arrays.asList(list));
                //добавление поднятия на уровень выше(продумать корень для юзера)!
                serverFiles.add(0, "..");
                return serverFiles;
            }
        }
        return List.of();
    }
    private void update(){
        serverFiles = new ArrayList<>(getServerFilesList(SERVER_DIR));
        sendUpdateServerList(serverFiles);
    }
    private void uploadFile(String fileRequestName) {
        String filePath = SERVER_DIR + "/" + fileRequestName;
        File file = new File(filePath);
        if(file.isFile()){
            try {
                        dos.writeUTF(Command.SEND_FILE_TO_USER.getCommand());
                        dos.writeUTF(fileRequestName);
                        dos.writeLong(file.length());
                System.out.println("file.length() " +file.length());
                try(FileInputStream fis = new FileInputStream(file);
                    BufferedInputStream bis = new BufferedInputStream(fis)){
                    int read;
                    while((read = bis.read(batch)) != -1){
                        System.out.println(read +"\n");
                        dos.write(batch,0,read);
                        dos.flush();
                    }

                }
                System.out.println("File transfer complete\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public FileHandler(Socket socket) throws IOException {
        this.socket = socket;
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
        //порция для буфера
        batch = new byte[BATCH_SIZE];

        //создание папки на сервере!
        File file = new File(SERVER_DIR);
        if (!file.exists()) {
            file.mkdir();
        }
        System.out.println("Client accepted...");
    }

    @Override
    public void run() {
        try {
            System.out.println("Start listening...");
                update();
            while (true) {
                String command = dis.readUTF();
                if (command.equals(Command.SEND_FILE.getCommand())) {
                    String fileName = dis.readUTF();
                    long size = dis.readLong();
                    try (FileOutputStream fos = new FileOutputStream(SERVER_DIR + "/" + fileName)) {
                        for (int i = 0; i < (size + BATCH_SIZE - 1) / BATCH_SIZE; i++) {
                            int read = dis.read(batch);
                            fos.write(batch, 0, read);
                        }
                        update();
                    } catch (Exception ignored) {}
                } else if (command.equals(Command.GET_FILE.getCommand())) {
                    try {
                        String fileRequestName = dis.readUTF();
                        uploadFile(fileRequestName);
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception ignored) {
            System.out.println("Client disconnected...");
        }
    }

}
