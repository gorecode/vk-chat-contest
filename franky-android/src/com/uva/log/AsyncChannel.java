//
// Copyright 2010 by UVaResearch Corp.
// Do not redistribute.
//

package com.uva.log;

import java.util.Vector;

/**
 * A channel uses a separate thread for logging.<br>
 * Using this channel can help to improve the performance of applications that produce huge amounts of log messages or that write log messages to multiple channels simultaneously.<br>
 * All log messages are put into a queue and this queue is then processed by a separate thread.
 * @author enikey.
 */
final public class AsyncChannel implements Channel {
  /**
   * Creates async channel with given underlying channel and starts background thread.
   * @param channel the underlying channel that processes log messages in background thread.
   * @throws IllegalArgumentException if given channel is null.
   */
  public AsyncChannel(Channel channel) {
    if (channel == null) {
      throw new IllegalArgumentException("Given channel for async channel is null");
    }
    _channel = channel;
    _queue = new Vector();
    _thread = new Thread(new Runnable() { public void run() { doQueueProcessingLoop(); } }, "AsyncChannel");
    _thread.start();
  }

  /**
   * Puts given log message into queue for later processing in background thread.
   * @param msg the log message.
   */
  public void log(Message msg) {
    synchronized (_queue) {
      _queue.addElement(msg);
      _queue.notify();
    }
  }

  /**
   * Interrupts and joins background thread and closes underlying channel after.
   */
  public void close() {
    _thread.interrupt();

    try {
      _thread.join();
    } catch (InterruptedException shouldNeverHappen) {
      Thread.currentThread().interrupt();
    }

    _channel.close();
  }

  /**
   * Runs message queue processing loop.
   * <p>
   * Loop can be breaked only by _thread.interrupt() call.
   */
  private void doQueueProcessingLoop() {
    try {
      while (true) {
        Message messageToProcess = null;
        synchronized (_queue) {
          if (_queue.size() == 0) {
            _queue.wait();
          }
          messageToProcess = (Message)_queue.firstElement();
          _queue.removeElementAt(0);
        } // synchronized (_queue).
        try {
          _channel.log(messageToProcess);
        } catch (RuntimeException unexpected) {
          ; // Do nothing, but it bad situation when real channel raises exception.
        }
      } // while (not canceled).
    } catch (InterruptedException interrupted) {
      // We can't anything useful here, so just exit.
    }
  } // void run.

  /** Message's queue. */
  private final Vector _queue;
  /** Working thread. */
  private final Thread _thread;
  /** Real logging channel. */
  private final Channel _channel;
}
