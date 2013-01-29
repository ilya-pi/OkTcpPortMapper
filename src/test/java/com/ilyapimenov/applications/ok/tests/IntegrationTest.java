package com.ilyapimenov.applications.ok.tests;

import com.ilyapimenov.applications.ok.TcpPortMapper;
import com.ilyapimenov.applications.ok.util.ConfParser;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static junit.framework.Assert.assertTrue;

/**
 * @author Ilya Pimenov
 *         <p/>
 *         Tests whether we would be able to access ok.ru through localhost:8080 provided correct configuration
 */
public class IntegrationTest {

    private static String confString =
            "ok.localPort = 8080\n" +
                    "ok.remoteHost = www.ok.ru\n" +
                    "rock.remoteHost = www.odnoklassniki.ru\n" +
                    "ok.remotePort = 80\n";

    @Before
    public void setup() throws Exception {
        new Thread() {
            @Override
            public void run() {
                try {
                    new TcpPortMapper().start(ConfParser.parse(new ByteArrayInputStream(confString.getBytes())));
                } catch (Exception e) {
                    /* ignore */
                }
            }
        }.start();
        Thread.sleep(1000);
    }

    @Test
    public void testOk() throws IOException {
        HttpClient httpClient = new HttpClient();
        GetMethod getMethod = new GetMethod("http://localhost:8080");
        httpClient.executeMethod(getMethod);
        //todo refine, check for a better trigger once internet is back again
        assertTrue(getMethod.getResponseBodyAsString().contains("Одноклассники"));
    }

}
