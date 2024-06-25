package com.carlos

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import javax.imageio.ImageIO
import java.io.File
import java.io.IOException

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
    try {
        val processBuilder = ProcessBuilder("tesseract", imagePath, "stdout")
        processBuilder.redirectErrorStream(true)

        val process = processBuilder.start()
        process.waitFor()

        return process.inputStream.bufferedReader().readText()
    } catch(e: IOException) {
        e.printStackTrace()
    } catch(e: InterruptedException) {
        e.printStackTrace()
    }
    return "Error processing image with Tesseract"
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