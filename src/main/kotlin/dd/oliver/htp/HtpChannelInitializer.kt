package dd.oliver.htp

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.stream.ChunkedWriteHandler

class HtpChannelInitializer(val basePath: String) : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        ch.pipeline().addLast("HttpCodec", HttpServerCodec())
        ch.pipeline().addLast("HttpAggregator", HttpObjectAggregator(65536))
        ch.pipeline().addLast("HttpChunked", ChunkedWriteHandler())
        ch.pipeline().addLast("RequestHandler", RequestHandler(basePath))
    }
}