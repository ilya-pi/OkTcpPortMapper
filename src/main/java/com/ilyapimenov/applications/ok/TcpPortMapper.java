package com.ilyapimenov.applications.ok;

import com.ilyapimenov.applications.ok.util.ConfParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Ilya Pimenov
 *         <p/>
 *         Simple tcp port mapper implementation with java nio (thread per connection approach)
 */
public class TcpPortMapper {

    private static final Logger LOG = LoggerFactory.getLogger(ConfParser.class);

    public static void main(String args[]) throws Exception {
        InputStream confStream = (args.length >= 1 ? new FileInputStream(args[0]) : Thread.currentThread().getContextClassLoader().getResourceAsStream("proxy.properties"));
        new TcpPortMapper().start(ConfParser.parse(confStream));
    }

    public void start(Map<Integer, InetSocketAddress> mappings) throws Exception {

        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 128, 10000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(1000));

        Selector selector = Selector.open();

        for (Integer port : mappings.keySet()) {
            ServerSocketChannel portListener = ServerSocketChannel.open();
            portListener.configureBlocking(false);
            portListener.socket().bind(new InetSocketAddress(port));
            portListener.register(selector, SelectionKey.OP_ACCEPT, port);
        }

        while (true) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            for (Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext(); ) {
                SelectionKey selected = i.next();
                i.remove();
                if (selected.isAcceptable()) {
                    makeLove(
                            ((ServerSocketChannel) selected.channel()).accept(),
                            SocketChannel.open(mappings.get(selected.attachment())),
                            executor
                    );
                }
            }
        }
    }

    private void makeLove(final SocketChannel from, final SocketChannel to, final ThreadPoolExecutor executor) {
        /**
         * currently it is thread-per connection; which is rather "ok", but bad under a high load.
         *
         * i would guess it is better to use a single selector to handle a number of connections,
         * in one thread, while storing info required to handle it nicely in the SelectionKey.attachement()
         *
         * ultimately cool is to switch from thread-per connection architecture under medium load,
         * to a single thread per a number of connections under heavy-load, just like couch db par example does
         *
         * but this would require me way more then three hours in summary
         */
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

    boolean forwardAll(SocketChannel from, SocketChannel to, ByteBuffer buff) throws IOException {
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
