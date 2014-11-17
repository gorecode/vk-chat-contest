//
// Copyright 2010 by UVaResearch Corp.
// Do not redistribute.
//

package com.uva.net;

import java.io.IOException;

/**
 * An abstract factory for creating HttpConnection instances.<br>
 * Must be implemented in platform-dependent subclasses.
 * @author enikey.
 * @category platform-dependent.
 */
public abstract class HttpConnector {
  /**
   * Http GET request method.
   */
  public final static String GET_METHOD = "GET";
  /**
   * Http POST request method.
   */
  public final static String POST_METHOD = "POST";

  /**
   * Establishes http connection to the specified url.
   * @param host target host.
   * @param port target port.
   * @param path target path.
   * @param method can be GET_METHOD or POST_METHOD.
   * @return ready to use HttpConnection.
   * @throws IOException when io-error occurs.
   */
  public HttpConnection openHttpConnection(String host, int port, String path, String method) throws IOException {
    return openHttpConnection("http", host, port, path, method);
  }

  public HttpConnection openHttpConnection(String scheme, String host, int port, String path, String method) throws IOException {
    return openHttpConnection(scheme + "://" + host + ":" + port + path, method);
  }

  /**
   * Establishes http connection with speicified url.
   * @param url target url.
   * @param method can be GET_METHOD or POST_METHOD.
   * @return ready to use HttpConnection.
   * @throws IOException when io-error occurs.
   */
  public abstract HttpConnection openHttpConnection(String url, String method)
    throws IOException;
}
