package com.company.simplednsclient.core;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Future;

public class SimpleDnsClient {
    private AsynchronousSocketChannel clientChannel;
    private Future writeResult;
    private Future readResult;
    private String query;
    private InetSocketAddress hostAddress;
    private Future connectResult;
    private Boolean waitingForResponse;
    private int messageId;
    private ByteBuffer buffer;

    public SimpleDnsClient() { }

    public void run() throws InterruptedException {
        init();

        for (;;) {
            check();
            System.out.print(".");
            Thread.sleep(100);
        }
    }

    private void check() {
        checkSendQuery();
        checkSendQueryDone();
        checkResponseReceived();
    }

    public void init() {
        messageId = 0;
        waitingForResponse = false;
        System.out.println("Client is started.");
        hostAddress = new InetSocketAddress("localhost", 3883);
    }

    public void checkSendQuery() {
        if (waitingForResponse)
            return;

        try {
            if (clientChannel == null) {
                System.out.println("Connecting to DNS server");
                clientChannel = AsynchronousSocketChannel.open();
                connectResult = clientChannel.connect(hostAddress);
            }

            if (connectResult != null && connectResult.isDone() && writeResult == null) {
                connectResult.get();
                connectResult = null;
                System.out.println("Connected to DNS server");
                messageId++;
                query = "Message with messsage Id: " + messageId;
                byte[] messageByteArray = query.getBytes();
                ByteBuffer buffer = ByteBuffer.wrap(messageByteArray);
                writeResult = clientChannel.write(buffer);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void checkSendQueryDone() {
        try {
            if (writeResult != null && writeResult.isDone()) {
                writeResult.get();
                writeResult = null;
                System.out.println("Send message: " + query);
                waitingForResponse = true;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void checkResponseReceived() {
        if (!waitingForResponse)
            return;

        try {
            if (readResult == null) {
                buffer = ByteBuffer.allocate(1024);
                readResult = clientChannel.read(buffer);
            }

            if (readResult.isDone()) {
                readResult.get();
                readResult = null;
                String message = new String(buffer.array()).trim();
                System.out.println("Received response: " + message);
                clientChannel.close();
                clientChannel = null;
                waitingForResponse = false;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
