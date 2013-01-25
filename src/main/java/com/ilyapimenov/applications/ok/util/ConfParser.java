package com.ilyapimenov.applications.ok.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Ilya Pimenov
 *         <p/>
 *         Sample configuration set for a single record:
 *         web.localPort = 8080
 *         web.remoteHost = www.odnoklassniki.ru
 *         web.remotePort = 80
 *         <p/>
 *         order of localPort/remoteHost/remotePort is not important,
 *         validations against
 *         1) missing not complete configuration (par example, "remoteHost" is missing)
 *         2) unrecognized records (par example, type in "localPrt")
 */
public class ConfParser {

    public static void main(String args[]) throws IOException {
        System.out.println(parse(Thread.currentThread().getContextClassLoader().getResourceAsStream("proxy.properties")));
    }

    public static Map<Integer, InetSocketAddress> parse(InputStream data) throws IOException {
        Properties prop;
        (prop = new Properties()).load(data);

        Map<String, ProxyConfTriad> triads = new HashMap<String, ProxyConfTriad>();
        for (String p : prop.stringPropertyNames()) {
            String[] keys = p.split("\\.");

            if (!triads.containsKey(keys[0])) {
                triads.put(keys[0], new ProxyConfTriad());
            }
            if (keys[1] == null || !ProxyProp.contains(keys[1])) {
                System.out.println(String.format("Unrecognizable record %s", p));
                continue;
            }
            //note: in java 7 you can have switch statements on String
            switch (ProxyProp.valueOf(keys[1])) {
                case localPort:
                    triads.get(keys[0]).port = Integer.parseInt(prop.getProperty(p));
                    break;
                case remoteHost:
                    triads.get(keys[0]).host = prop.getProperty(p);
                    break;
                case remotePort:
                    triads.get(keys[0]).hostPort = Integer.parseInt(prop.getProperty(p));
                    break;
            }
        }

        Map<Integer, InetSocketAddress> result = new HashMap<Integer, InetSocketAddress>();
        for (String key : triads.keySet()) {
            ProxyConfTriad t = triads.get(key);
            if (!t.isComplete()) {
                System.out.println(String.format("Warning! Configuration for %s is incomplete, please check proxy.properties file", key));
            } else {
                result.put(t.port, new InetSocketAddress(t.host, t.hostPort));
            }
        }
        return result;
    }

    private enum ProxyProp {
        localPort, remoteHost, remotePort;

        public static boolean contains(String value) {
            for (ProxyProp p : ProxyProp.values()) {
                if (p.name().equals(value)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class ProxyConfTriad {
        Integer port;
        String host;
        Integer hostPort;

        boolean isComplete() {
            return port != null && host != null && hostPort != null;
        }
    }

}
