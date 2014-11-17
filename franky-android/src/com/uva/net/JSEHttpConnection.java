//
// Copyright 2010 by UVaResearch Corp.
// Do not redistribute.
//

package com.uva.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.uva.log.Log;
import com.uva.net.HttpConnection;

/**
 * A HttpConnection implementation for the Java SE platform.<br>
 * Uses java.net.HttpURLConnection inside.
 * @author enikey.
 */
public class JSEHttpConnection extends HttpConnection {
  private static final String TAG = "JSEHttpConnection";

  /**
   * Creates http connection with given uri and request method.
   * @param url the url.
   * @param method the request method ("GET" or "POST").
   * @throws IllegalArgumentException if passed parameters are invalid.
   * @throws IOException if I/O error occurs.
   */
  public JSEHttpConnection(String url, String method) throws IOException {
    createConnection(url, method);
  }

  /**
   * Creates http connection with given url parts and request method.
   * @param host the url hostname.
   * @param port the url port.
   * @param path the url path.
   * @param method the request method ("GET" or "POST").
   * @throws IllegalArgumentException if passed parameters are invalid.
   * @throws IOException if I/O error occurs.
   */
  public JSEHttpConnection(String host, int port, String path, String method) throws IOException {
    if (host == null) {
      throw new IllegalArgumentException("Host is null");
    }
    if (port < 0) {
      throw new IllegalArgumentException("Port is negative");
    }

    createConnection("http://" + host + ":" + port + path, method);
  }

  /**
   * {@inheritDoc}
   */
  public OutputStream openOutputStream() throws IOException {
    connectIfNeeded();
    Log.trace(TAG, "Opening http connection output stream");
    return _connection.getOutputStream();
  }

  /**
   * {@inheritDoc}
   */
  public InputStream openInputStream() throws IOException {
    connectIfNeeded();
    Log.trace(TAG, "Opening http connection input stream");
    return _connection.getInputStream();
  }

  /**
   * {@inheritDoc}
   */
  public void close() throws IOException {
    Log.trace(TAG, "Closing http connection");
    _connection.disconnect();
  }

  /**
   * Initializes http connection with given URL and request method ("POST" or "GET") and connects.
   * <p>
   * <i>Note</i>: uses 10s timeout for reading and connecting and user-agent taken from CoreEnv.getDeviceInfo.getDeviceUserAgentResolver().
   * @param targetUrl valid target Url.
   * @param method request method.
   * @throws IOException when io error occurs.
   */
  private void createConnection(String url, String method) throws IOException {
    if ((method.compareTo("GET") != 0) && (method.compareTo("POST") != 0)) {
      throw new IllegalArgumentException("Unknown http request method");
    }

    URL targetUrl = null;

    try {
      targetUrl = new URL(url.replace(" ", "%20"));
    } catch (MalformedURLException urlEx) {
      throw new IllegalArgumentException("Invalid hostname");
    }

    Log.debug(TAG, "Creating http connection to " + targetUrl.toString() + " using " + method + " method");

    boolean doOutput = (method.compareTo("POST") == 0);

    _connection = (HttpURLConnection)targetUrl.openConnection();
    _connection.setRequestMethod(method);
    _connection.setDefaultUseCaches(false);
    _connection.setUseCaches(false);
    _connection.setConnectTimeout(30 * 1000);
    _connection.setReadTimeout(30 * 1000);
    _connection.setDoOutput(doOutput);
    _connection.setDoInput(true);
  }

  private void connectIfNeeded() throws IOException {
    if (!_connected) {
      fillConnectionWithProperties();
      _connection.connect();
      _connected = true;
    }
  }

  private void fillConnectionWithProperties() {
    Log.debug(TAG, "Filling connection with properties");
    HttpConnection.RequestProperties properties = requestProperties();
    String[] keys = properties.keys();
    for (int i = 0; i < keys.length; i++) {
      String field = keys[i];
      String value = properties.get(field);
      Log.debug(TAG, "Using property: " + field + ": " + value);
      _connection.addRequestProperty(field, value);
    }
  }

  private HttpURLConnection _connection;
  private boolean _connected;
}
