package com.houvven.guise.util.android

object ShellExecutor {

    fun execute(command: String, asRoot: Boolean): Result<String> = runCatching {
        require(command.isNotBlank()) { "command must not be blank" }

        val process = ProcessBuilder(if (asRoot) "su" else "sh")
            .redirectErrorStream(true)
            .start()

        process.outputStream.bufferedWriter().use { writer ->
            writer.appendLine(command)
            writer.appendLine("exit")
        }

        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        check(exitCode == 0) { output.ifBlank { "Shell exited with code $exitCode" } }
        output
    }
}
