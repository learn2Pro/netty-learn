package org.learn2pro.netty.http.server;

import static io.netty.handler.codec.http.HttpUtil.is100ContinueExpected;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class NettyHttpServer {

  int port;

  public NettyHttpServer(int port) {
    this.port = port;
  }

  public void serve() throws Exception {
    ServerBootstrap bootstrap = new ServerBootstrap();
    EventLoopGroup boss = new NioEventLoopGroup(1);
    EventLoopGroup work = new NioEventLoopGroup();
    bootstrap.group(boss, work)
        .handler(new LoggingHandler(LogLevel.DEBUG))
        .channel(NioServerSocketChannel.class)
        .childHandler(new HttpServerInitializer());

    ChannelFuture f = bootstrap.bind(new InetSocketAddress(port)).sync();
    System.out.println(" server start up on port : " + port);
    f.channel().closeFuture().sync();
  }

  public class HttpServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
      ChannelPipeline pipeline = channel.pipeline();
      pipeline.addLast(new HttpServerCodec());// http 编解码
      pipeline.addLast("httpAggregator", new HttpObjectAggregator(512
          * 1024)); // http 消息聚合器                                                                     512*1024为接收的最大contentlength
      pipeline.addLast(new HttpRequestHandler());// 请求处理器

    }
  }

  public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
      ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
      //100 Continue
      if (is100ContinueExpected(req)) {
        ctx.write(new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.CONTINUE));
      }
      // 获取请求的uri
      String uri = req.uri();
      Map<String, String> resMap = new HashMap<>();
      resMap.put("method", req.method().name());
      resMap.put("uri", uri);
      String msg = "hello world! 你请求uri为：" + uri;
      // 创建http响应
      FullHttpResponse response = new DefaultFullHttpResponse(
          HttpVersion.HTTP_1_1,
          HttpResponseStatus.OK,
          Unpooled.copiedBuffer(msg, StandardCharsets.UTF_8));
      // 设置头信息
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/json; charset=UTF-8");
      // 将html write到客户端
      ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
  }
}
