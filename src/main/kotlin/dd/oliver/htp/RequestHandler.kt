package dd.oliver.htp

import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import org.slf4j.LoggerFactory
import java.io.RandomAccessFile

private val logger = LoggerFactory.getLogger(RequestHandler::class.java)

class RequestHandler(val basePath: String) : SimpleChannelInboundHandler<HttpRequest>() {
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.error("Server encounter error:")
        cause.printStackTrace()
        sendError(
            ctx, "Something bad happen to the server",
            HttpResponseStatus.INTERNAL_SERVER_ERROR
        )
        ctx.channel().close()
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        logger.debug("A client connected")
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.debug("A client disconnected")
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpRequest) {
        val path = basePath + msg.uri()
        val file = RandomAccessFile(path, "r")
        DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.PARTIAL_CONTENT)
        if (msg.headers().contains("Range")) { // Multiple threads download
            println("Find Range: ${msg.headers().get("Range")}")
            val range = msg.headers().get("Range").substring(6) // remove 'bytes='
            if (range.indexOf("-") == range.length - 1) { // example: 0-

            } else { // example: 0-100

            }
        } else { // Single thread download

        }

    }

    private fun sendError(ctx: ChannelHandlerContext, msg: String, status: HttpResponseStatus) {
        val bufAllocator = ctx.alloc()
        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status)
        val buffer = bufAllocator.buffer(msg.length)
        buffer.writeBytes(msg.toByteArray())
        response.content().writeBytes(buffer)
        buffer.release()
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
    }

}