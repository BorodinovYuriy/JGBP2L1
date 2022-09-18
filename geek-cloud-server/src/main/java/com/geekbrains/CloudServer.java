package com.geekbrains;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ThreadFactory;

public class CloudServer {

    public static void main(String[] args){

        ThreadFactory serviceThreadFactory = r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            System.out.println("Номер потока для клиента: "+thread.getName()+"\n");
            return thread;
        };

        try(ServerSocket serverSocket = new ServerSocket(8189)) {
            System.out.println("Server started!");
            while (true) {

                Socket socket = serverSocket.accept();
                serviceThreadFactory.newThread(new FileHandler(socket))
                        .start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
