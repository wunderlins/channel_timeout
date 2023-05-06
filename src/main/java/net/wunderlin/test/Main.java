package net.wunderlin.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class Main {

    // create nio socket channel
    public static SocketChannel create_channel(String host, int port, int timeout) throws IOException {
        // instantiate a channel
        SocketChannel channel = SocketChannel.open();
        // set socket timeout
        channel.socket().setSoTimeout(timeout);
        // connect to the server
        channel.connect(new InetSocketAddress(host, port));
        // set the channel to non-blocking
        channel.configureBlocking(false);
        // return the channel
        return channel;
    }

    // main method
    public static void main(String[] args) throws IOException {
        System.out.println("connecting to localhost:9999, timeout 30 seconds");

        // create a channel
        SocketChannel channel = create_channel("localhost", 9999, 30000);

    }
}
