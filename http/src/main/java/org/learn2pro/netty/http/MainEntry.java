package org.learn2pro.netty.http;

import org.learn2pro.netty.http.server.NettyHttpServer;

public class MainEntry {

  public static void main(String[] args) {
    try {
      System.out.println("start netty http server!");
      new NettyHttpServer(8081).serve();
    } catch (Exception e) {
      System.err.println("start netty http server failed!");
    }
  }

}
