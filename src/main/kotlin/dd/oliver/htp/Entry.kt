package dd.oliver.htp

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.path
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Main")

class Htp : CliktCommand() {
    init {
        versionOption("1.0.0")
    }

    private val path by argument(help = "Path to display").path(mustExist = true, canBeFile = false)

    override fun run() {
        val file = path.toFile()
        val canPath = file.canonicalPath.replace("\\", "/")
        logger.info("Pick up path $canPath")

        val htpServer = HtpServer(canPath)
        htpServer.run(2333)
    }
}

fun main(args: Array<String>) = Htp().main(args)