package com.carlos

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.IOException

fun searchInPDFs(dirPath: String, searchTerm: String) {
    val dir = File(dirPath)
    val pdfFiles = dir.listFiles { _, name -> name.endsWith(".pdf") }

    pdfFiles?.forEach { file ->
        try {
            PDDocument.load(file).use { document ->
                val pdfStripper = PDFTextStripper()
                val text = pdfStripper.getText(document)
                if (searchTerm in text) {
                    println("Found '$searchTerm' in '$file.name'")
                }
            }
        } catch (e: IOException) {
            println("Error processing PDF: ${e.message} in ${file.name}")
        }
    }
}

fun main() {
    print("Enter path to directory: ")
    val dirPath: String = readlnOrNull() ?: ""
    print("Enter search term: ")
    val searchTerm: String = readlnOrNull() ?: ""

    if (dirPath.isEmpty() || searchTerm.isEmpty()) {
        return
    } else {
        searchInPDFs(dirPath, searchTerm)
    }
}