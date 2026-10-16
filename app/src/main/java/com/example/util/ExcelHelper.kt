package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipInputStream

object ExcelHelper {

    private const val TAG = "ExcelHelper"

    /**
     * Share or export an Excel (.xls) or CSV (.csv) file using Android Chooser.
     */
    fun shareFile(
        context: Context,
        fileName: String,
        fileContent: String,
        mimeType: String = "application/vnd.ms-excel",
        dialogTitle: String = "Buka / Bagikan File Excel"
    ) {
        try {
            val cacheDir = context.cacheDir
            val exportDir = File(cacheDir, "excel_exports").apply { mkdirs() }
            val file = File(exportDir, fileName)
            file.writeText(fileContent, Charsets.UTF_8)

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Template Import Stok & Inventori Obat Apotek FARMIVA ($fileName)"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, dialogTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Save the file to public Downloads or application storage directory
     */
    fun saveToDownloads(
        context: Context,
        fileName: String,
        fileContent: String
    ): String {
        return try {
            val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            if (downloadDir != null && (downloadDir.exists() || downloadDir.mkdirs())) {
                val destFile = File(downloadDir, fileName)
                destFile.writeText(fileContent, Charsets.UTF_8)
                destFile.absolutePath
            } else {
                val appFilesDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                    ?: context.filesDir
                val destFile = File(appFilesDir, fileName)
                destFile.writeText(fileContent, Charsets.UTF_8)
                destFile.absolutePath
            }
        } catch (_: Exception) {
            val fallbackFile = File(context.filesDir, fileName)
            fallbackFile.writeText(fileContent, Charsets.UTF_8)
            fallbackFile.absolutePath
        }
    }

    /**
     * High-Capacity File Reader:
     * Automatically inspects file format and reads:
     * 1. Modern Excel (.xlsx) files via built-in ZipInputStream + XmlPullParser
     * 2. Excel 2003 XML (.xls)
     * 3. CSV, TSV, Semicolon-delimited files (UTF-8, ISO-8859-1, Windows-1252)
     */
    fun readTextFromUri(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()

            if (isXlsxZipFile(bytes)) {
                Log.d(TAG, "Detected modern Excel .xlsx file (${bytes.size} bytes). Parsing XML sheets...")
                val parsedCsv = parseXlsxBytes(bytes)
                if (!parsedCsv.isNullOrBlank()) {
                    return parsedCsv
                }
            }

            // Fallback for CSV, XML (.xls), or plain text
            decodeText(bytes)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file from uri: $uri", e)
            null
        }
    }

    /**
     * Check if bytes start with ZIP magic bytes (PK\u0003\u0004) used by .xlsx
     */
    private fun isXlsxZipFile(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
                bytes[0] == 0x50.toByte() &&
                bytes[1] == 0x4B.toByte() &&
                bytes[2] == 0x03.toByte() &&
                bytes[3] == 0x04.toByte()
    }

    /**
     * Native Android .xlsx Parser with Zero Third-Party Dependencies.
     * Extracts sharedStrings.xml and sheet1.xml from the ZIP package and
     * formats them into standard multi-line CSV text.
     */
    private fun parseXlsxBytes(bytes: ByteArray): String? {
        return try {
            var sharedStringsBytes: ByteArray? = null
            var sheetBytes: ByteArray? = null

            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val entryName = entry.name.lowercase()
                    if (entryName.contains("sharedstrings.xml")) {
                        sharedStringsBytes = zip.readBytes()
                    } else if (entryName.contains("worksheets/sheet1.xml") ||
                        (entryName.contains("worksheets/sheet") && entryName.endsWith(".xml") && sheetBytes == null)
                    ) {
                        sheetBytes = zip.readBytes()
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            if (sheetBytes == null) {
                Log.w(TAG, "No sheet XML found in .xlsx archive")
                return null
            }

            // 1. Parse Shared Strings table
            val sharedStrings = mutableListOf<String>()
            if (sharedStringsBytes != null) {
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(ByteArrayInputStream(sharedStringsBytes), "UTF-8")

                var event = parser.eventType
                var inSi = false
                var inT = false
                var textBuffer = StringBuilder()

                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            if (parser.name.equals("si", ignoreCase = true)) {
                                inSi = true
                                textBuffer = StringBuilder()
                            } else if (inSi && parser.name.equals("t", ignoreCase = true)) {
                                inT = true
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inSi && inT) {
                                textBuffer.append(parser.text)
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name.equals("t", ignoreCase = true)) {
                                inT = false
                            } else if (parser.name.equals("si", ignoreCase = true)) {
                                inSi = false
                                sharedStrings.add(textBuffer.toString())
                            }
                        }
                    }
                    event = parser.next()
                }
            }

            // 2. Parse Worksheet rows & cells
            val sheetFactory = XmlPullParserFactory.newInstance()
            val sheetParser = sheetFactory.newPullParser()
            sheetParser.setInput(ByteArrayInputStream(sheetBytes), "UTF-8")

            val csvBuilder = StringBuilder()
            val rowCells = mutableListOf<String>()
            var inRow = false
            var inCell = false
            var inValue = false
            var cellType = ""
            var cellCol = -1
            var cellValue = StringBuilder()

            var sheetEvent = sheetParser.eventType
            while (sheetEvent != XmlPullParser.END_DOCUMENT) {
                when (sheetEvent) {
                    XmlPullParser.START_TAG -> {
                        when (sheetParser.name.lowercase()) {
                            "row" -> {
                                inRow = true
                                rowCells.clear()
                            }
                            "c" -> {
                                inCell = true
                                val cellRef = sheetParser.getAttributeValue(null, "r") ?: ""
                                cellCol = getColumnIndexFromRef(cellRef)
                                cellType = sheetParser.getAttributeValue(null, "t") ?: ""
                                cellValue = StringBuilder()
                            }
                            "v", "t" -> {
                                if (inCell) inValue = true
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inCell && inValue) {
                            cellValue.append(sheetParser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (sheetParser.name.lowercase()) {
                            "v", "t" -> inValue = false
                            "c" -> {
                                inCell = false
                                val raw = cellValue.toString().trim()
                                val resolved = if (cellType == "s") {
                                    val idx = raw.toIntOrNull() ?: -1
                                    if (idx in sharedStrings.indices) sharedStrings[idx] else raw
                                } else if (cellType == "b") {
                                    if (raw == "1") "true" else "false"
                                } else {
                                    raw
                                }
                                if (cellCol >= 0) {
                                    while (rowCells.size < cellCol) {
                                        rowCells.add("")
                                    }
                                }
                                rowCells.add(resolved)
                            }
                            "row" -> {
                                inRow = false
                                if (rowCells.any { it.isNotBlank() }) {
                                    val line = rowCells.joinToString(",") { cell ->
                                        if (cell.contains(",") || cell.contains("\"") || cell.contains("\n") || cell.contains(";")) {
                                            "\"${cell.replace("\"", "\"\"")}\""
                                        } else {
                                            cell
                                        }
                                    }
                                    csvBuilder.append(line).append("\n")
                                }
                            }
                        }
                    }
                }
                sheetEvent = sheetParser.next()
            }

            val finalCsv = csvBuilder.toString()
            Log.d(TAG, "Successfully extracted .xlsx file into CSV with ${finalCsv.lines().size} lines")
            finalCsv
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing .xlsx archive", e)
            null
        }
    }

    private fun getColumnIndexFromRef(ref: String?): Int {
        if (ref.isNullOrBlank()) return -1
        var col = 0
        var foundLetter = false
        for (ch in ref) {
            if (ch in 'A'..'Z') {
                foundLetter = true
                col = col * 26 + (ch - 'A' + 1)
            } else if (ch in 'a'..'z') {
                foundLetter = true
                col = col * 26 + (ch - 'a' + 1)
            } else {
                break
            }
        }
        return if (foundLetter) col - 1 else -1
    }

    private fun decodeText(bytes: ByteArray): String {
        return try {
            String(bytes, Charsets.UTF_8)
        } catch (_: Exception) {
            try {
                String(bytes, Charsets.ISO_8859_1)
            } catch (_: Exception) {
                String(bytes)
            }
        }
    }
}
