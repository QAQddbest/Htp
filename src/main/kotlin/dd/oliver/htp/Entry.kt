package dd.oliver.htp

import org.slf4j.LoggerFactory
import java.io.File

private val logger = LoggerFactory.getLogger("Main")

fun main(args: Array<String>) {

    val path: String
    if (args.isEmpty()) {
        logger.error("Path is needed")
        return
    } else {
        val file = File(args[0])
        if (!file.exists()) {
            logger.error("Path is not exists")
            return
        }
        if (!file.isDirectory) {
            logger.error("Path is not directory")
            return
        }
        path = file.canonicalPath.replace("\\", "/")
        logger.info("Pick up path $path")
    }
    val htpServer = HtpServer(path)
    htpServer.run(2333)
}