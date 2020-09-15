package dd.oliver.htp

import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import org.slf4j.LoggerFactory
import java.io.File
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
        logger.trace("A client connected")
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.trace("A client disconnected")
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpRequest) {
        if (msg.uri() == "/favicon.ico") {
            // Line
            val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
            // Headers
            response.headers().set("Content-Type", "image/vnd.microsoft.icon")
            // Content
            response.content().writeBytes(RequestHandler::class.java.getResourceAsStream("/img/file.png").readBytes())
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
        } else {
            val path = basePath + msg.uri() // msg.uri() example: / or /a/b or /a/b/c.txt
            logger.debug("Fetching $path")
            val file = File(path)
            if (file.isFile) {
                val rfile = RandomAccessFile(file, "r")
                if (msg.headers().contains("Range")) { // Multiple threads download
                    logger.debug("Find 'Range' in HttpRequest: ${msg.headers().get("Range")}")
                    val range = msg.headers().get("Range").substring(6) // remove 'bytes='
                    // Line
                    val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.PARTIAL_CONTENT)
                    val bIdx = range.substring(0, range.indexOf("-")).toLong()
                    var eIdx = 0L
                    if (range.indexOf("-") == range.length - 1) { // example: 0-
                        // TODO: Judge bIdx > file.size?
                        eIdx = rfile.length() - 1
                    } else { // example: 0-100
                        eIdx = range.substring(range.indexOf("-") + 1, range.length).toLong()
                    }
                    logger.debug("bIdx = ${bIdx}; eIdx = ${eIdx}")
                    // Headers
                    response.headers().set("Accept-Ranges", "bytes")
                    response.headers().set("Content-Range", "bytes ${bIdx}-${eIdx}/${rfile.length()}")
                    response.headers().set("Content-Disposition", "attachment; filename=\"${file.name}\"")
                    response.headers().set("Content-Type", "application/octet-stream")
                    response.headers().set("Content-Length", "${eIdx - bIdx + 1}")
                    // Content
                    response.content().writeBytes(rfile.channel, bIdx, (eIdx - bIdx + 1).toInt())
                    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
                } else { // Single thread download
                    logger.debug("No 'Range' in HttpRequest")
                    // Line
                    val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
                    // Headers
                    response.headers().set("Accept-Ranges", "bytes")
                    response.headers().set("Content-Range", "bytes ${0}-${rfile.length() - 1}/${rfile.length()}")
                    response.headers().set("Content-Disposition", "attachment; filename=\"${file.name}\"")
                    response.headers().set("Content-Type", "application/octet-stream")
                    response.headers().set("Content-Length", "${rfile.length()}")
                    // Content
                    response.content().writeBytes(rfile.channel, 0L, rfile.length().toInt())
                    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
                }
            } else {
                val builder = StringBuilder()
                builder.append(
                    """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>Htp Server</title>
                </head>
                <body>
                    <dl>
                """.trimIndent()
                )
                file.list().forEach {
                    builder.append("<dt><a href=\"${msg.uri() + it + "/"}\">${it}</a></dt>")
                }
                builder.append(
                    """
                    </dl>
                </body>
                </html>
            """.trimIndent()
                )
                // Line
                val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
                // Headers
                response.headers().set("Content-Type", "text/html; charset=utf-8")
                // Content
                response.content().writeBytes(builder.toString().toByteArray())
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
            }
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