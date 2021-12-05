package gh.marad.chi.utils

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

fun String.runCommand(workingDir: File): String =
    split(" ").toTypedArray().runCommand(workingDir)

fun Array<String>.runCommand(workingDir: File): String {
    val process = ProcessBuilder(*this)
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()
    val text = process.inputStream.bufferedReader().readText()
    val error = process.errorStream.bufferedReader().readText()
    process.waitFor(60, TimeUnit.MINUTES)
    println(process.exitValue())
    if (process.exitValue() != 0) {
        println(error)
        println(text)
        throw RuntimeException("Run failed")
    }
    return text
}

fun deleteDirectory(path: Path) {
    if (Files.exists(path)) {
        Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .map { it.toFile() }
            .forEach { it.delete() }
    }
}
