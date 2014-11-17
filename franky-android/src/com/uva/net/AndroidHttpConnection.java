package com.uva.net;

import java.io.IOException;

import com.uva.net.JSEHttpConnection;

/**
 * HttpConnection implementation for the Android platform.
 * <p>
 * <b>Covered Android issue description:</b><br>
 * HttpURLConnection class can share sockets between different http connections when same URL used.<br>
 * Read more on http://stackoverflow.com/questions/1440957/httpurlconnection-getresponsecode-returns-1-on-second-invocation.
 * @author enikey.
 * @deprecated class uses "black magic" to avoid Android OS issue, class will be removed as soon as replacement of it will be written.
 */
public class AndroidHttpConnection extends JSEHttpConnection {
  /**
   * {@inheritDoc}
   */
  public AndroidHttpConnection(String url, String method) throws IOException {
    super(url, method);
    fixAndroidSpecificIssue();
  }

  /**
   * Fixes android-specific issue with sharing sockets between http connections.
   */
  private static void fixAndroidSpecificIssue() {
    System.setProperty("http.keepAlive", "false");
  }
}
