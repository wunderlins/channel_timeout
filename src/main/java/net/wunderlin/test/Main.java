package net.wunderlin.test;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;

public class Main {

    // instantiate static logger
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(Main.class);

    /**
     * main method
     * @param args
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(String[] args) {

        /**
         * This example shows how to handle listen timeouts with java.nio.Channel
         * when non-blocking connections are used.
         */
        int listen_timeout = (int) 30_000;
        try {
            channel_listen_timeout(listen_timeout);
        } catch (SocketTimeoutException e) {
            logger.error("timeout");
            return;
        } catch (IOException e) {
            logger.error("error", e);
        }

    }

    private static void channel_listen_timeout(int listen_timeout) throws IOException, SocketTimeoutException {
        logger.info("Listening on localhost: 9999, timeout {} seconds", (int) (listen_timeout / 1000));
        
        // prepare listener
        Selector selector = Selector.open();
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress("localhost", 9999));

        // serverSocket.socket().setSoTimeout(listen_timeout); // -> does not work
        serverSocket.configureBlocking(false);

        // register accept 
        int ops = serverSocket.validOps();
        serverSocket.register(selector, ops);
        ByteBuffer buffer = ByteBuffer.allocate(256);

        logger.info("ready to accept connection (timeout {}) ...", (int) (listen_timeout / 1000));

        // start a thread and sleep for 2 seconds
        new Thread(() -> {
            logger.info("sleeping {} seconds", (int) ((listen_timeout + 2_000) / 1000));
            try {
                Thread.sleep(listen_timeout + 2_000);
            } catch (InterruptedException e) {
                logger.error("error sleeping", e);
            }
            logger.info("waking up");
        }).start();
        
        // current time
        long start = System.currentTimeMillis();

        // wait for selector to be fired, if not within timeout (start + listen_timeout)
        // then throw a timeout exception
        SelectionKey key = null;
        while (true) {

            // check if we have reached the timeout
            if (System.currentTimeMillis() > start + listen_timeout) {
                serverSocket.close();
                throw new SocketTimeoutException();
            }

            logger.info("selecting ...");
            if (selector.select(100) <= 0) continue; // non-blocking
            logger.info("processing selection ...");
            
            // we got one or more event(s)
            // refresh timeout and handle it
            start = System.currentTimeMillis();

            // handle events
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();
            while (iter.hasNext()) {

                key = iter.next();

                if (key.isAcceptable()) {
                    register(selector, serverSocket);
                }

                if (key.isReadable()) {
                    answerWithEcho(buffer, key);
                }
                iter.remove();
            }

            logger.info("next round ...");
        }
    }

    private static void register(Selector selector, ServerSocketChannel serverSocket)
      throws IOException {
 
        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_CONNECT);
    }

    private static void answerWithEcho(ByteBuffer buffer, SelectionKey key)
    throws IOException {

      SocketChannel client = (SocketChannel) key.channel();
      int r = client.read(buffer);
      if (r == -1 || new String(buffer.array()).trim().equals("exit")) {
          client.close();
          System.out.println("Not accepting client messages anymore");
      }
      else {
          buffer.flip();
          client.write(buffer);
          buffer.clear();
      }
  }
}
