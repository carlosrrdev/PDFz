package com.carlos

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import javax.imageio.ImageIO
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun extractTextFromPDF(pdfPath: String, keyword: String) {
    val doc = PDDocument.load(File(pdfPath))
    val renderer = PDFRenderer(doc)
    var keywordFound = false

    for (i in 0 until doc.numberOfPages) {

        // Convert each page in the PDF into images
        val image = renderer.renderImageWithDPI(i, 300F)
        val tempImage = File("page$i.png")
        ImageIO.write(image, "PNG", tempImage)

        val text = runTess(tempImage.absolutePath)
        if(keyword in text) {
            keywordFound = true
        }
        tempImage.delete() // Delete temporary file
    }

    doc.close()
    if(keywordFound) {
        println("Keyword '$keyword' found in: $pdfPath")
    }
}

fun runTess(imagePath: String): String {
  val executor = Executors.newSingleThreadExecutor()
    val future = executor.submit<String> {
        try {
            val processBuilder = ProcessBuilder("tesseract", imagePath, "stdout")
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()
            if(!process.waitFor(1, TimeUnit.MINUTES)) {
                process.destroy()
                throw RuntimeException("Process timeout: OCR process was terminated due to exceeding time limit")
            }
            process.inputStream.bufferedReader().readText()
        } catch(e: IOException) {
            e.printStackTrace()
            "Error processing image with Tesseract"
        } catch(e: InterruptedException) {
            e.printStackTrace()
            "Error processing image with Tesseract"
        }
    }

    try {
        return future.get(1, TimeUnit.MINUTES)
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

    pdfFiles?.forEach { file ->
        extractTextFromPDF(file.absolutePath, keyword)
    } ?: println("No PDF files found in $directoryPath")
}

fun main() {
    println("Enter the path to the directory containing PDF files:")
    val directoryPath = readlnOrNull() ?: return println("No input provided for directory path.")

    println("Enter the keyword to search for in the PDF files:")
    val keyword = readlnOrNull() ?: return println("No input provided for keyword.")

    processDirectory(directoryPath, keyword)
}