package net.dean.gbs.api.test

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.InputStreamReader
import java.io.BufferedReader

public class ProcessOutputAdapter {
    class object {
        private val logger: Logger = LoggerFactory.getLogger("gradle-build")
    }

    public fun attach(p: Process, name: String) {
        Gobbler(p.getInputStream(), "$name-stdout", false).start()
        Gobbler(p.getErrorStream(), "$name-stderr", true).start()
    }

    private inner class Gobbler(input: InputStream, val buildName: String, val error: Boolean) : Thread() {
        val reader = BufferedReader(InputStreamReader(input))

        override fun run() {
            reader.forEachLine {
                val msg = "<$buildName> $it"
                if (error)
                    logger.error(msg)
                else
                    logger.info(msg)
            }
        }
    }
}
