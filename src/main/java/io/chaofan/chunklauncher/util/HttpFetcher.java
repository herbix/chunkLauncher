package io.chaofan.chunklauncher.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;

import java.net.HttpURLConnection;

import org.json.JSONObject;
import io.chaofan.chunklauncher.Config;

/**
 * This class contains methods to easy access http contents.
 *
 * @author Chaos
 * @since ChunkLauncher 1.3.2
 */
public final class HttpFetcher {

    private static final JProgressBar defpb = new JProgressBar();
    private static JProgressBar progress = defpb;

    public static String lastErrorResponse = null;

    public static void setJProgressBar(JProgressBar p) {
        if (p == null) {
            progress = defpb;
        }
        progress = p;
    }

    private static HttpURLConnection createConnection(String url, String method, int downloaded, int len, String type)
            throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", type + "; charset=utf-8");
        return createConnection(url, method, downloaded, len, headers);
    }

    private static HttpURLConnection createConnection(String url, String method, int downloaded, int len, Map<String, String> headers)
            throws IOException {
        HttpURLConnection conn;
        URL console = new URL(url);
        if (Config.enableProxy && Config.proxy != null) {
            conn = (HttpURLConnection) console.openConnection(Config.proxy);
        } else {
            conn = (HttpURLConnection) console.openConnection();
        }
        conn.setRequestMethod(method);
        if (downloaded > 0) {
            conn.addRequestProperty("Range", downloaded + "-");
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            conn.addRequestProperty(entry.getKey(), entry.getValue());
        }
        if (len > 0) {
            conn.addRequestProperty("Content-Length", String.valueOf(len));
            conn.setUseCaches(false);
            conn.setDoOutput(true);
        }
        return conn;
    }

    public static int pipeStream(InputStream in, OutputStream out) throws IOException {
        return pipeStream(in, out, 0, -1);
    }

    private static int pipeStream(InputStream in, OutputStream out, int downloaded, int length) throws IOException {
        byte[] buffer = new byte[4096];
        int count;

        int n = 0;
        if (length != 0) {
            n = downloaded * 100 / length;
        }

        while ((count = in.read(buffer)) >= 0) {
            downloaded += count;
            if (count > 0) {
                out.write(buffer, 0, count);
                if (length != 0) {
                    int newN = downloaded * 100 / length;
                    increaseProgressValue(newN - n);
                    n = newN;
                }
            }
        }

        increaseProgressValue(100 - n);

        return downloaded;
    }

    private static void increaseProgressValue(final int value) {
        if (value > 0) {
            SwingUtilities.invokeLater(() -> progress.setValue(progress.getValue() + value));
        }
    }

    /**
     * Get content from the url.
     *
     * @param url The url
     * @return The content. If exception occurs, <i>null</i> will be returned.
     */
    public static String fetch(String url) {
        return fetch(url, null);
    }

    /**
     * Get content from the url.
     *
     * @param url     The url
     * @param headers Http headers
     * @return The content. If exception occurs, <i>null</i> will be returned.
     */
    public static String fetch(String url, Map<String, String> headers) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        boolean failed = fetchToStream(url, out, headers);

        if (failed)
            return null;
        return new String(out.toByteArray());
    }

    /**
     * Get content from the url and save to a local file.
     * Be sure <b>param file</b> is a file that doesn't exist.
     *
     * @param url  The url
     * @param file Local filename to save
     * @return Whether network access is available.
     * @throws IOException if the file can't be created
     */
    public static boolean fetchAndSave(String url, String file) throws IOException {
        OutputStream out = new FileOutputStream(file);

        boolean failed = fetchToStream(url, out, null);

        out.close();

        if (failed) {
            new File(file).delete();
            return false;
        }
        return true;
    }

    /**
     * Get content from the url and output to a stream.
     *
     * @param url The url
     * @param out Stream to output
     * @param headers Http headers
     * @return Whether the operation succeed
     */
    public static boolean fetchToStream(String url, OutputStream out, Map<String, String> headers) {
        boolean failed;
        int tryCount = 0;
        int downloaded = 0;
        int length = -1;
        do {
            tryCount++;
            HttpURLConnection conn = null;
            failed = false;
            try {
                if (headers == null) {
                    conn = createConnection(url, "GET", downloaded, 0, "application/x-www-form-urlencoded");
                } else {
                    conn = createConnection(url, "GET", downloaded, 0, headers);
                }
                conn.connect();
                if (length == -1) {
                    String lenStr = conn.getHeaderField("Content-Length");
                    if (lenStr == null)
                        length = -2;
                    else
                        length = Integer.valueOf(lenStr);
                }

                if (conn.getResponseCode() >= 400) {
                    lastErrorResponse = null;
                    InputStream in = conn.getErrorStream();
                    ByteArrayOutputStream newOut = new ByteArrayOutputStream();
                    pipeStream(in, newOut);
                    in.close();
                    lastErrorResponse = new String(newOut.toByteArray());
                    failed = true;
                    if (conn.getResponseCode() < 500) {
                        break;
                    }
                } else {
                    InputStream in = conn.getInputStream();
                    pipeStream(in, out, downloaded, length);
                    in.close();
                }
            } catch (Exception e) {
                failed = true;
            } finally {
                if (conn != null)
                    conn.disconnect();
            }
        } while (failed && tryCount < 10);

        return failed;
    }

    /**
     * Get content from the url, using POST method.
     *
     * @param url    The url
     * @param params The map contains post params
     * @return The content. If exception occurs, <i>null</i> will be returned.
     */
    public static String fetchUsePostMethod(String url, Map<String, String> params) {
        return fetchUsePostMethod(url, URLParam.mapToParamString(params), "application/x-www-form-urlencoded");
    }

    /**
     * Get content from the url, using POST method.
     *
     * @param url  The url
     * @param json The JSON object to send
     * @return The content. If exception occurs, <i>null</i> will be returned.
     */
    public static String fetchUsePostMethod(String url, JSONObject json) {
        return fetchUsePostMethod(url, json.toString(), "application/json");
    }

    /**
     * Get content from the url, using POST method.
     *
     * @param url    The url
     * @param params The string contains post params
     * @return The content. If exception occurs, <i>null</i> will be returned.
     */
    public static String fetchUsePostMethod(String url, String params) {
        return fetchUsePostMethod(url, params, "application/x-www-form-urlencoded");
    }

    /**
     * Get content from the url, using POST method.
     *
     * @param url    The url
     * @param params The string contains post params
     * @param type   The param mime type
     * @return The content. If exception occurs, <i>null</i> will be returned.
     */
    public static String fetchUsePostMethod(String url, String params, String type) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", type + "; charset=utf-8");
        return fetchUsePostMethod(url, params, headers);
    }

    /**
     * Get content from the url, using POST method.
     *
     * @param url     The url
     * @param params  The string contains post params
     * @param headers Http headers
     * @return The content. If exception occurs, <i>null</i> will be returned.
     */
    public static String fetchUsePostMethod(String url, String params, Map<String, String> headers) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        boolean failed;
        int tryCount = 0;
        do {
            tryCount++;
            HttpURLConnection conn = null;
            failed = false;
            try {
                byte[] toSend = params.getBytes(StandardCharsets.UTF_8);
                conn = createConnection(url, "POST", 0, toSend.length, headers);
                conn.connect();

                OutputStream os = conn.getOutputStream();
                os.write(toSend);
                os.flush();
                os.close();

                if (conn.getResponseCode() >= 400) {
                    lastErrorResponse = null;
                    InputStream in = conn.getErrorStream();
                    pipeStream(in, out);
                    in.close();
                    lastErrorResponse = new String(out.toByteArray());
                    failed = true;
                    if (conn.getResponseCode() < 500) {
                        break;
                    }
                } else {
                    InputStream in = conn.getInputStream();
                    pipeStream(in, out);
                    in.close();
                }
            } catch (Exception e) {
                failed = true;
            } finally {
                if (conn != null)
                    conn.disconnect();
            }
        } while (failed && tryCount < 10);

        if (failed)
            return null;
        return new String(out.toByteArray());
    }

}
