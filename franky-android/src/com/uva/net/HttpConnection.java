//
// Copyright 2010 by UVaResearch Corp.
// Do not redistribute.
//

package com.uva.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import com.uva.lang.StringUtilities;

/**
 * An abstract HTTP connection.<br>
 * Must be implemented in platform-dependent subclasses.
 * HttpConnection instance is used to make a single request to the HTTP server.
 * @author enikey.
 * @category platform-dependent.
 */
public abstract class HttpConnection {
  public final RequestProperties requestProperties() {
    return m_requestProperties;
  }

  /**
   * Opens an input stream from which the data in the response may be read.
   * @return an InputStream.
   * @throws IOException if io-error occurs.
   */
  abstract public InputStream openInputStream() throws IOException;
  /**
   * Gets an output stream which can be used send an entity with the request.<br>
   * @return an OutputStream.
   * @throws IOException if io-error occurs.
   */
  abstract public OutputStream openOutputStream() throws IOException;
  /**
   * Closes this HttpConnection.
   * @throws IOException when io-error occurs.
   */
  abstract public void close() throws IOException;

  private final RequestProperties m_requestProperties = new RequestProperties();

  public static class RequestProperties {
    public void set(String name, String value) {
      if (name == null) {
        throw new IllegalArgumentException("Name of header cannot be null");
      }
      if (value == null) {
        throw new IllegalArgumentException("Value of header cannot be null");
      }
      for (int i = 0; i < m_properties.size(); i++) {
        Pair each = (Pair)m_properties.elementAt(i);
        if (StringUtilities.equalsIgnoreCase(each.key, name)) {
          each.value = value;
          return;
        }
      }
      m_properties.addElement(new Pair(name, value));
    }

    public String get(String name) {
      for (int i = 0; i < m_properties.size(); i++) {
        Pair each = (Pair)m_properties.elementAt(i);
        if (StringUtilities.equalsIgnoreCase(each.key, name)) {
          return each.value;
        }
      }
      return null;
    }

    public void remove(String name) {
      for (int i = 0; i < m_properties.size(); i++) {
        Pair each = (Pair)m_properties.elementAt(i);
        if (StringUtilities.equalsIgnoreCase(each.key, name)) {
          m_properties.removeElementAt(i);
          return;
        }
      }
    }

    public void removeAll() {
      m_properties.removeAllElements();
    }

    public String[] keys() {
      final int propertiesCount = m_properties.size(); 
      final String[] keys = new String[propertiesCount];
      for (int i = 0; i < propertiesCount; i++) {
        keys[i] = ((Pair)(m_properties.elementAt(i))).key;
      }
      return keys;
    }

    private final Vector m_properties = new Vector();

    private static class Pair {
      public Pair(String key, String value) {
        this.key = key;
        this.value = value;
      }
      public String key;
      public String value;
    }
  }
}
