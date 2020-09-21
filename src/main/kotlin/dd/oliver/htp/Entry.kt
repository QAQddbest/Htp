package dd.oliver.htp

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import org.slf4j.LoggerFactory
import java.net.InetAddress


private val logger = LoggerFactory.getLogger("Main")

class Htp : CliktCommand() {
    init {
        versionOption("1.0.1")
    }

    private val port by option("-p", "--port", help = "Port to deploy").int().default(2333)
    private val path by argument(help = "Path to display").path(mustExist = true, canBeFile = false)

    override fun run() {
        val file = path.toFile()
        val canPath = file.canonicalPath.replace("\\", "/")
        logger.info("Pick up path $canPath")
        val addr = InetAddress.getLocalHost()
        logger.info("Now browse http://${addr.hostAddress}:${port} to use service")
        val htpServer = HtpServer(canPath)
        htpServer.run(port)
    }
}

fun main(args: Array<String>) = Htp().main(args)