package com.ilyapimenov.applications.ok;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class App {

    public static void main(String args[]) throws Exception {
        new App().start();
    }

    public void start() throws Exception {

        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 128, 10000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(1000));

        Selector selector = Selector.open();

        ServerSocketChannel acceptingServerSocketChannel = ServerSocketChannel.open();
        acceptingServerSocketChannel.configureBlocking(false);
        acceptingServerSocketChannel.socket().bind(new InetSocketAddress(8080));
        acceptingServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            for (Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext(); ) {
                SelectionKey selected = i.next();
                i.remove();
                if (selected.isAcceptable()) {
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selected.channel();
                    SocketChannel fromChannel = serverSocketChannel.accept();
                    InetSocketAddress toAddres = new InetSocketAddress("www.ok.ru", 80);
                    makeLove(fromChannel, SocketChannel.open(toAddres), executor);
                }
            }
        }
    }

    private void makeLove(final SocketChannel from, final SocketChannel to, final ThreadPoolExecutor executor) throws Exception {
        executor.execute(new Runnable() {
            public void run() {
                Selector selector = null;
                ByteBuffer buff = ByteBuffer.allocate(1024);

                try {
                    selector = Selector.open();

                    from.configureBlocking(false);
                    from.register(selector, SelectionKey.OP_READ);
                    to.configureBlocking(false);
                    to.register(selector, SelectionKey.OP_READ);

                    while (selector.select() > 0) {
                        Set<SelectionKey> selectedKeys = selector.selectedKeys();
                        for (Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext(); ) {
                            SelectionKey key = i.next();
                            i.remove();
                            SocketChannel readySocketChannel = (SocketChannel) key.channel();
                            if (readySocketChannel == null) {
                                continue;
                            }
                            if (key.isReadable()) {
                                if (forwardAll(readySocketChannel, (readySocketChannel == from ? to : from), buff)) {
                                    return;
                                }
                            }
                        }
                    }
                } catch (ClosedChannelException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (selector != null) {
                        try {
                            selector.close();
                        } catch (IOException e) {
                        }
                    }
                    if (from != null) {
                        try {
                            from.close();
                        } catch (IOException e) {
                        }
                    }
                    if (to != null) {
                        try {
                            to.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        });
    }

    boolean forwardAll(SocketChannel from, SocketChannel to, ByteBuffer buff) throws Exception {
        buff.clear();
        while (true) {
            int read = from.read(buff);
            if (read > 0) {
                buff.flip();
                to.write(buff);
                buff.flip();
            } else if (read == 0) {
                return false;
            } else if (read < 0) {
                return true;
            }
        }
    }

}
