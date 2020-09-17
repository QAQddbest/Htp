package dd.oliver.htp

import io.netty.channel.*
import io.netty.handler.codec.http.*
import io.netty.handler.stream.ChunkedFile
import org.slf4j.LoggerFactory
import java.io.File
import java.io.RandomAccessFile


private val logger = LoggerFactory.getLogger(RequestHandler::class.java)

class RequestHandler(val basePath: String) : SimpleChannelInboundHandler<HttpRequest>() {
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.error("Server encounter error:")
        cause.printStackTrace()
        sendError(
            ctx,
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
        logger.info("request: ${msg.uri()}")
        if (msg.uri() == "/favicon.ico") {
            // Line
            val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
            // Headers
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "image/vnd.microsoft.icon")
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            // Content
            response.content().writeBytes(RequestHandler::class.java.getResourceAsStream("/img/file.png").readBytes())
            val future = ctx.writeAndFlush(response)
            future.addListener(ChannelFutureListener.CLOSE)
        } else {
            val path = basePath + msg.uri() // msg.uri() example: / or /a/b or /a/b/c.txt
            val file = File(path)
            if (file.isFile) {
                val rfile = RandomAccessFile(file, "r")
                if (msg.headers().contains("Range")) { // Multiple threads download
                    logger.debug("Find 'Range' in HttpRequest: ${msg.headers().get("Range")}")
                    val range = msg.headers().get("Range").substring(6) // remove 'bytes='
                    // Line
                    val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.PARTIAL_CONTENT)
                    val bIdx = range.substring(0, range.indexOf("-")).toLong()
                    var eIdx = 0L
                    eIdx = if (range.indexOf("-") == range.length - 1) { // example: 0-
                        // TODO: Judge bIdx > file.size?
                        rfile.length() - 1
                    } else { // example: 0-100
                        range.substring(range.indexOf("-") + 1, range.length).toLong()
                    }
                    logger.debug("bIdx = ${bIdx}; eIdx = ${eIdx}")
                    // Headers
                    response.headers().set(HttpHeaderNames.ACCEPT_RANGES, HttpHeaderValues.BYTES)
                    response.headers().set(HttpHeaderNames.CONTENT_DISPOSITION, "attachment; filename=\"${file.name}\"")
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_OCTET_STREAM)
                    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, "${eIdx - bIdx + 1}")
                    response.headers().set(HttpHeaderNames.CONTENT_RANGE, "bytes ${bIdx}-${eIdx}/${rfile.length()}")
                    if (!HttpUtil.isKeepAlive(msg)) {
                        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
                    } else {
                        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                    }
                    // Content
                    ctx.channel().write(response)
                    val sendFileFuture = ctx.write(
                        HttpChunkedInput(ChunkedFile(rfile, bIdx, eIdx - bIdx + 1, 8192)),
                        ctx.newProgressivePromise()
                    )
                    sendFileFuture.addListener(object : ChannelProgressiveFutureListener {
                        override fun operationProgressed(
                            future: ChannelProgressiveFuture,
                            progress: Long,
                            total: Long
                        ) {
                            if (total < 0) { // total unknown
                                logger.trace(future.channel().toString() + " Transfer progress: " + progress)
                            } else {
                                logger.trace(
                                    future.channel().toString() + " Transfer progress: " + progress + " / " + total
                                )
                            }
                        }

                        override fun operationComplete(future: ChannelProgressiveFuture) {
                            logger.trace(future.channel().toString() + " Transfer complete.")
                        }
                    })
                    if (!HttpUtil.isKeepAlive(msg)) {
                        sendFileFuture.addListener(ChannelFutureListener.CLOSE)
                    }
                } else { // Single thread download
                    logger.debug("No 'Range' in HttpRequest")
                    // Line
                    val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
                    // Headers
                    response.headers().set(HttpHeaderNames.ACCEPT_RANGES, HttpHeaderValues.BYTES)
                    response.headers().set(HttpHeaderNames.CONTENT_DISPOSITION, "attachment; filename=\"${file.name}\"")
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_OCTET_STREAM)
                    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, "${rfile.length()}")
                    if (!HttpUtil.isKeepAlive(msg)) {
                        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
                    } else {
                        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                    }
                    // Content
                    ctx.write(response)
                    val sendFileFuture = ctx.write(
                        HttpChunkedInput(ChunkedFile(rfile, 0, rfile.length(), 8192)),
                        ctx.newProgressivePromise()
                    )
                    sendFileFuture.addListener(object : ChannelProgressiveFutureListener {
                        override fun operationProgressed(
                            future: ChannelProgressiveFuture,
                            progress: Long,
                            total: Long
                        ) {
                            if (total < 0) { // total unknown
                                logger.trace(future.channel().toString() + " Transfer progress: " + progress)
                            } else {
                                logger.trace(
                                    future.channel().toString() + " Transfer progress: " + progress + " / " + total
                                )
                            }
                        }

                        override fun operationComplete(future: ChannelProgressiveFuture) {
                            logger.trace(future.channel().toString() + " Transfer complete.")
                        }
                    })
                    if (!HttpUtil.isKeepAlive(msg)) {
                        sendFileFuture.addListener(ChannelFutureListener.CLOSE)
                    }
                }
            } else if (file.isDirectory) {
                sendHtml(ctx, HttpResponseStatus.OK) {
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
                    if (file.list().size == 0) {
                        builder.append("<h1>Empty here</h1>")
                    } else {
                        file.list().forEach {
                            builder.append("<dt><a href=\"${msg.uri() + it + "/"}\">${it}</a></dt>")
                        }
                    }
                    builder.append(
                        """
                    </dl>
                </body>
                </html>
            """.trimIndent()
                    )
                    builder.toString()
                }
            } else {
                sendHtml(ctx, HttpResponseStatus.BAD_GATEWAY) {
                    val builder = StringBuilder()
                    builder.append(
                        """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="UTF-8">
                <title>Bad request</title>
                </head>
                <body>
                    <h1>404</h1>
                    Path error...
                </body>
                </html>
                """.trimIndent()
                    )
                    builder.toString()
                }
            }
        }
    }

    private fun sendHtml(ctx: ChannelHandlerContext, status: HttpResponseStatus, function: () -> String) {
        val result = function()
        // Line
        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status)
        // Headers
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_HTML)
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        // Content
        response.content().writeBytes(result.toByteArray())
        val future = ctx.writeAndFlush(response)
        future.addListener(ChannelFutureListener.CLOSE)
    }

    private fun sendError(ctx: ChannelHandlerContext, status: HttpResponseStatus) {
        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status)
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
    }

}