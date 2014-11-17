package com.uva.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface AbstractSocketConnection {
	public void setSoTimeout(int timeout) throws IOException;
	public int getSoTimeout() throws IOException;
	public InputStream openInputStream() throws IOException;
	public OutputStream openOutputStream() throws IOException;
	public void close() throws IOException;
}
