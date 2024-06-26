package com.carlos

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import javax.imageio.ImageIO
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object AnsiColor {
    const val RESET = "\u001B[0m"
    const val RED = "\u001B[31m"
    const val GREEN = "\u001B[32m"
    const val YELLOW = "\u001B[33m"
    const val BLUE = "\u001B[34m"
    const val PURPLE = "\u001B[35m"
    const val CYAN = "\u001B[36m"
    const val WHITE = "\u001B[37m"
}

fun extractTextFromPDF(pdfPath: String, keyword: String): Boolean {
    val doc = PDDocument.load(File(pdfPath))
    val renderer = PDFRenderer(doc)

    doc.use { document ->
        for (i in 0 until document.numberOfPages) {

            // Convert each page in the PDF into images
            val image = renderer.renderImageWithDPI(i, 300F)
            val tempImage = File("page$i.png")
            ImageIO.write(image, "PNG", tempImage)

            val text = runTess(tempImage.absolutePath)
            tempImage.delete() // Delete temporary file

            if(keyword in text) {
                return true
            }
        }
    }
    return false
}

fun runTess(imagePath: String): String {
  val executor = Executors.newSingleThreadExecutor()
    val future = executor.submit<String> {
        try {
            val processBuilder = ProcessBuilder("tesseract", imagePath, "stdout")
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()
            if(!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroy()
                throw RuntimeException("Process timeout: OCR process was terminated due to exceeding time limit")
            }
            process.inputStream.bufferedReader().readText()
        } catch(e: IOException) {
//            e.printStackTrace()
            "Error processing image with Tesseract"
        } catch(e: InterruptedException) {
//            e.printStackTrace()
            "Page timeout reached, skipping..."
        }
    }

    try {
        return future.get(10, TimeUnit.SECONDS)
    } catch(e: Exception) {
        future.cancel(true)
        return "OCR process canceled due to timeout"
    } finally {
        executor.shutdownNow()
    }
}

fun processDirectory(directoryPath: String, keyword: String) {
    val directory = File(directoryPath)
    val pdfFiles = directory.listFiles { _, name -> name.endsWith(".pdf", ignoreCase = true) }

    if(pdfFiles.isNullOrEmpty()) {
        println("No PDF files found")
        return;
    }
    val maxThreads = 4
    val threads = Runtime.getRuntime().availableProcessors()
    val threadCount = minOf(maxThreads, threads)
    val executor = Executors.newFixedThreadPool(threadCount)

    pdfFiles.forEach { file ->
        executor.submit {
            if(extractTextFromPDF(file.absolutePath, keyword)) {
                println("\n${AnsiColor.GREEN}Keyword: $keyword found in: ${file.absolutePath}${AnsiColor.RESET}")
            }
        }
    }

    executor.shutdown()
    try {
        if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
            executor.shutdownNow()
        }
    } catch(e: InterruptedException) {
        executor.shutdownNow()
        Thread.currentThread().interrupt()
    }
}

fun main() {
    println("Enter the path to the directory containing PDF files:")
    val directoryPath = readlnOrNull() ?: return println("No input provided for directory path.")

    println("Enter the keyword to search for in the PDF files:")
    val keyword = readlnOrNull() ?: return println("No input provided for keyword.")

    val spinner = Spinner("Processing PDFS:")
    spinner.start()

    try {
        processDirectory(directoryPath, keyword)
    } finally {
        spinner.stop()
    }
}