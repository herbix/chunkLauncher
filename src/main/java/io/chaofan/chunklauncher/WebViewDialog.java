package io.chaofan.chunklauncher;

import io.chaofan.chunklauncher.util.HttpFetcher;
import io.chaofan.chunklauncher.util.Lang;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;

import javafx.application.Platform;
import javafx.embed.swing.*;
import javafx.scene.Scene;
import javafx.scene.web.WebView;

public class WebViewDialog extends JDialog {

    private JPanel base = new JPanel();
    private JFXPanel fxPanel = new JFXPanel();
    public WebView webView;

    public WebViewDialog() {
        setResizable(false);
        setModal(true);

        base.setPreferredSize(new Dimension(600, 600));

        add(base);
        pack();
        createFrame();
        setTitle(Lang.getString("ui.update.title"));

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screen.width - getWidth()) / 2, (screen.height - getHeight()) / 2);
    }

    private void createFrame() {
        base.setLayout(new BorderLayout());
        base.add(fxPanel);

        Platform.runLater(() -> {
            webView = new WebView();
            fxPanel.setScene(new Scene(webView));
        });

        workaroundForWebView();
    }

    private static void workaroundForWebView() {
        try {
            URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
                @Override
                public URLStreamHandler createURLStreamHandler(String protocol) {
                    if ("https".equals(protocol)) {

                        URLStreamHandler handler = null;
                        try {
                            Object o = Class.forName("sun.net.www.protocol.https.Handler").newInstance();
                            handler = (URLStreamHandler)o;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        URLStreamHandler finalHandler = handler;
                        Method method;
                        try {
                            method = finalHandler.getClass().getDeclaredMethod("openConnection", URL.class, Proxy.class);
                            method.setAccessible(true);
                        } catch (NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }

                        return new URLStreamHandler() {
                            @Override
                            protected URLConnection openConnection(URL u) throws IOException {
                                return this.openConnection(u, Proxy.NO_PROXY);
                            }

                            @Override
                            protected URLConnection openConnection(URL url, Proxy proxy) throws IOException {
                                System.out.println("openConnection " + url);

                                if (url.toExternalForm().endsWith("/common/handlers/watson")) {
                                    System.out.println("Failed: form calls watson");
                                }
                                final HttpsURLConnection httpsURLConnection;
                                try {
                                    httpsURLConnection = (HttpsURLConnection) method.invoke(finalHandler, url, proxy);
                                } catch (Exception e) {
                                    throw new IOException(e);
                                }
                                if ("login.microsoftonline.com".equals(url.getHost())
                                        && "/consumers/oauth2/v2.0/authorize".equals(url.getPath())) {

                                    return new URLConnection(url) {
                                        @Override
                                        public void connect() throws IOException {
                                            httpsURLConnection.connect();
                                        }

                                        public InputStream getInputStream() throws IOException {
                                            byte[] content = readFully(httpsURLConnection.getInputStream());
                                            String contentAsString = new String(content, "UTF-8");
                                            System.out.println(contentAsString);
                                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                            baos.write(contentAsString.replaceAll("integrity", "integrityDisabled").getBytes("UTF-8"));
                                            return new ByteArrayInputStream(baos.toByteArray());
                                        }

                                        private byte[] readFully(InputStream inputStream) throws IOException {
                                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                            HttpFetcher.pipeStream(inputStream, baos);
                                            return baos.toByteArray();
                                        }

                                        public OutputStream getOutputStream() throws IOException {
                                            return httpsURLConnection.getOutputStream();
                                        }

                                    };

                                } else {
                                    return httpsURLConnection;
                                }
                            }

                        };
                    }
                    return null;
                }
            });
        } catch (Throwable t) {
            System.out.println("Unable to register custom protocol handler");
        }
    }
}
