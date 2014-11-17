package com.uva.net;

import java.io.IOException;

import com.uva.net.HttpConnection;
import com.uva.net.HttpConnector;

/**
 * HttpConnector implementation for the Android platform.
 * @author enikey.
 * @category platform-dependent.
 */
public class AndroidHttpConnector extends HttpConnector {
  /**
   * {@inheritDoc}
   */
  public HttpConnection openHttpConnection(String url, String method) throws IOException {
    return new AndroidHttpConnection(url, method);
  }
}
