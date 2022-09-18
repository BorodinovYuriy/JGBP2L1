package com.geekbrains.sep22.geekcloudclient;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.*;

//Initializable - гарантия не пустых объектов ListView....
public class CloudMainController implements Initializable {
    private static final Integer BATCH_SIZE = 1024;
    private byte[] batch;
    public ListView<String> clientView;
    public ListView<String> serverView;
    private String currentDirectory;
    private DataInputStream dis;
    private DataOutputStream dos;
    private Socket socket;

    private void initNetwork() {
        try {
            socket = new Socket("localhost", 8189);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
        } catch (Exception ignored) {}
    }
    public void sendToServer(ActionEvent actionEvent) {
        String fileName = clientView.getSelectionModel().getSelectedItem();
        String filePath = currentDirectory + "/" + fileName;
        File file = new File(filePath);
        if (file.isFile()) {
            try {
                //реализовать отправку команд! (продумать обновление инфы сервера!!!)
                dos.writeUTF(Command.SEND_FILE.getCommand());
                dos.writeUTF(fileName);
                dos.writeLong(file.length());
                try (FileInputStream fis = new FileInputStream(file)) {
                    //реализовать буфер!
                    byte[] bytes = fis.readAllBytes();
                    dos.write(bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                System.err.println("e = " + e.getMessage());
            }
        }
    }
    public void getFromServer(){
        String downloadFileName = serverView.getSelectionModel().getSelectedItem();
        //полный путь пока опустил
        try {
            dos.writeUTF(Command.GET_FILE.getCommand());
            dos.writeUTF(downloadFileName);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    private void serverListener(){
        new Thread(new Thread(() -> {
            System.out.println("serverListenerThread is run\n");
            try{
                while (true){
                    String inputCommand = dis.readUTF();
                    if(inputCommand.equals(Command.UPDATE_FILE.getCommand())){
                        String serverUpdateString = dis.readUTF();
                        System.out.println(serverUpdateString);
                        List<String> serverUpdateList= new ArrayList<>(List.of(serverUpdateString.split("#")));
                        Platform.runLater(() -> updateServerView(serverUpdateList));

                    } else if (inputCommand.equals(Command.SEND_FILE_TO_USER.getCommand())) {
                            String downloadFileName = dis.readUTF();
                            long size = dis.readLong();
                                System.out.println(size+"\n");
                            String filePath = currentDirectory + "/" + downloadFileName;
                                System.out.println(filePath+"\n");
                        try(FileOutputStream fos = new FileOutputStream(filePath)){
                            try(BufferedInputStream bis = new BufferedInputStream(dis)){
                                batch = new byte[BATCH_SIZE];
                                int read;
                                while ((read = bis.read(batch)) != -1){
                                    System.out.println(read +"\n");
                                    fos.write(batch,0,read);
                                    fos.flush();
                                }
                            }
                        }

                    }


                }

            }catch (IOException e){
                    e.printStackTrace();

            }
            })).start();
    }
    private void updateServerView(List<String> serverUpdateList) {
        serverView.getItems().clear();
        serverView.getItems().addAll(serverUpdateList);

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initNetwork();
        //Открываем входящий поток для сервера
        serverListener();
        //открывает домашнюю папку
        setCurrentDirectory(System.getProperty("user.home"));

        fillView(clientView, getFiles(currentDirectory));
        clientView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = clientView.getSelectionModel().getSelectedItem();
                File selectedFile = new File(currentDirectory + "/" + selected);
                if (selectedFile.isDirectory()) {
                    setCurrentDirectory(currentDirectory + "/" + selected);
                }
            }
        });

    }

    private void setCurrentDirectory(String directory) {
        currentDirectory = directory;
        fillView(clientView, getFiles(currentDirectory));
    }

    private void fillView(ListView<String> view, List<String> data) {
        view.getItems().clear();
        view.getItems().addAll(data);
    }

    private List<String> getFiles(String directory) {
        File dir = new File(directory);
        if (dir.isDirectory()) {
            String[] list = dir.list();
            if (list != null) {
                List<String> files = new ArrayList<>(Arrays.asList(list));
                files.add(0, "..");
                return files;
            }
        }
        return List.of();
    }


}
