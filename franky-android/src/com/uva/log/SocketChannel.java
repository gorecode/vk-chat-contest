package com.uva.log;

import java.io.DataOutputStream;
import java.io.IOException;

import com.uva.net.AbstractSocketConnection;
import com.uva.net.PlatformSocketConnector;

public class SocketChannel implements Channel {
  private final AbstractSocketConnection m_socket;
  private final DataOutputStream m_socketOut;
  private boolean m_errorDuringWritting;

  public SocketChannel(String dstName, int dstPort, int timeout) throws IOException {
  	final PlatformSocketConnector connector = PlatformSocketConnector.getDefined();
    m_socket = connector.connect(dstName, dstPort, timeout);
    m_socketOut = new DataOutputStream(m_socket.openOutputStream());
  }

  public void log(Message msg) {
    if (m_errorDuringWritting) return;

    try {
      m_socketOut.writeUTF(msg.text() + "\n");
    } catch (IOException e) {
      m_errorDuringWritting = true;
    }
  }

  public void close() {
  	try {
  		m_socket.openInputStream().close();
  	} catch (IOException e) {
  		; // What can i do?
  	}

    try {
      m_socketOut.close();
    } catch (IOException ioE) {
      ; // Sorry.
    }

    try {
      m_socket.close();
    } catch (IOException ioE) {
      ; // Sorry.
    }
  }
}
