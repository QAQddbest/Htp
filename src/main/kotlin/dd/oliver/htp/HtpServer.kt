package dd.oliver.htp

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.net.InetSocketAddress

private val logger = LoggerFactory.getLogger(HtpServer::class.java)

class HtpServer(
    private var basePath: String,
    private var bossNum: Int = 1,
    private var workerNum: Int = Runtime.getRuntime().availableProcessors() * 2,
) : Closeable {

    private val bossGroupDelegate = lazy {
        NioEventLoopGroup(bossNum)
    }
    private val bossGroup: NioEventLoopGroup by bossGroupDelegate
    private val workerGroupDelegate = lazy {
        NioEventLoopGroup(workerNum)
    }
    private val workerGroup: NioEventLoopGroup by workerGroupDelegate

    fun run(port: Int) {
        val sbs = ServerBootstrap()
        sbs.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(HtpChannelInitializer(basePath))
        val channelFuture = sbs.bind(InetSocketAddress(port)).sync()
        logger.info("Server start at $port")
        channelFuture.channel().closeFuture().sync()

    }

    override fun close() {
        if (bossGroupDelegate.isInitialized())
            bossGroup.shutdownGracefully()
        if (workerGroupDelegate.isInitialized())
            workerGroup.shutdownGracefully()
    }

}