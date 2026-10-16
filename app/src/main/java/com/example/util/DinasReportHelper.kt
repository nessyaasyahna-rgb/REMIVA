package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.DrugWithBatches
import com.example.data.model.OpnameDiscrepancyItem
import com.example.data.model.PharmacyProfile
import com.example.data.model.StockBatch
import com.example.data.model.UserAccount
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DinasReportHelper {

    private val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    private fun formatRp(amount: Double): String = rupiahFormat.format(amount).replace("Rp", "Rp ")

    /**
     * Share or open a generated PDF file using FileProvider.
     */
    fun sharePdf(
        context: Context,
        file: File,
        dialogTitle: String = "Buka / Bagikan Berkas Laporan Dinas (PDF)"
    ) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Berkas Resmi Pelaporan Dinas Kesehatan & BPOM - Apotek FARMIVA (${file.name})"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, dialogTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Copy PDF to public Downloads directory and return the target path.
     */
    fun savePdfToDownloads(
        context: Context,
        fileName: String,
        sourceFile: File
    ): String {
        return try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destFile = if (downloadDir != null && (downloadDir.exists() || downloadDir.mkdirs())) {
                File(downloadDir, fileName)
            } else {
                val appFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                File(appFilesDir, fileName)
            }
            sourceFile.copyTo(destFile, overwrite = true)
            destFile.absolutePath
        } catch (_: Exception) {
            val fallbackFile = File(context.filesDir, fileName)
            sourceFile.copyTo(fallbackFile, overwrite = true)
            fallbackFile.absolutePath
        }
    }

    /**
     * Generate an official, multi-page vector PDF for Warehouse Inventory Report (Dinas Kesehatan & BPOM).
     */
    fun generateDinasWarehousePdf(
        context: Context,
        profile: PharmacyProfile,
        currentUser: UserAccount,
        warehouseItems: List<Pair<DrugWithBatches, StockBatch>>
    ): File {
        val document = PdfDocument()

        // Page setup: Standard A4 (595 x 842 points)
        val pageWidth = 595
        val pageHeight = 842
        val margin = 32f

        val titlePaint = Paint().apply {
            color = Color.rgb(27, 67, 50) // Dark Forest Green #1B4332
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.rgb(45, 106, 79) // #2D6A4F
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val metaPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 7.5f
            isAntiAlias = true
        }

        val headerCellPaint = Paint().apply {
            color = Color.WHITE
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val cellPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 7f
            isAntiAlias = true
        }

        val cellBoldPaint = Paint().apply {
            color = Color.rgb(27, 67, 50)
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 0.8f
        }

        val headerBgPaint = Paint().apply {
            color = Color.rgb(45, 106, 79) // #2D6A4F
            style = Paint.Style.FILL
        }

        val zebraBgPaint = Paint().apply {
            color = Color.rgb(244, 247, 244)
            style = Paint.Style.FILL
        }

        val rowsPerPage = 24
        val totalPages = ((warehouseItems.size + rowsPerPage - 1) / rowsPerPage).coerceAtLeast(1)
        val currentDateStr = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date())

        val totalUnits = warehouseItems.sumOf { it.second.gudangStock }
        val totalValuation = warehouseItems.sumOf { it.first.drug.buyPrice * it.second.gudangStock }

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            var y = margin + 12f

            // Top decorative bar
            val topBarPaint = Paint().apply {
                color = Color.rgb(45, 106, 79)
                style = Paint.Style.FILL
            }
            canvas.drawRect(margin, margin - 10f, pageWidth - margin, margin - 4f, topBarPaint)

            // Header (Kop Dokumen)
            canvas.drawText("BERKAS RESMI PELAPORAN INVENTARISASI GUDANG FARMASI", margin, y, titlePaint)
            y += 12f
            canvas.drawText(
                "DINAS KESEHATAN KOTA & BALAI BESAR PENGAWAS OBAT DAN MAKANAN (BPOM)",
                margin,
                y,
                subTitlePaint
            )
            y += 11f
            canvas.drawText(
                "Sarana: ${profile.pharmacyName}  |  No. SIA: ${profile.siaNumber}  |  No. SIPA: ${profile.sipaNumber}",
                margin,
                y,
                metaPaint
            )
            y += 10f
            canvas.drawText(
                "Alamat: ${profile.address}  |  Telp: ${profile.phone}  |  APA: ${profile.apotekerName}",
                margin,
                y,
                metaPaint
            )
            y += 10f
            canvas.drawText(
                "Tanggal Pelaporan: $currentDateStr WIB  |  Petugas Gudang / Pemeriksa: ${currentUser.fullName}",
                margin,
                y,
                metaPaint
            )
            y += 8f

            // Horizontal Divider
            val thickLinePaint = Paint().apply {
                color = Color.rgb(45, 106, 79)
                strokeWidth = 1.5f
            }
            canvas.drawLine(margin, y, pageWidth - margin, y, thickLinePaint)
            y += 12f

            // Table Column Widths (Sum = 531 ~= 595 - 64)
            val colNo = 20f
            val colBarcode = 60f
            val colName = 140f
            val colCat = 65f
            val colBatch = 50f
            val colExp = 45f
            val colStock = 35f
            val colUnit = 30f
            val colBuy = 45f
            val colVal = 50f

            // Draw Table Header Row
            val headerHeight = 18f
            canvas.drawRoundRect(
                RectF(margin, y - 10f, pageWidth - margin, y + headerHeight - 10f),
                3f,
                3f,
                headerBgPaint
            )

            var x = margin + 4f
            canvas.drawText("No", x, y + 2f, headerCellPaint); x += colNo
            canvas.drawText("Barcode", x, y + 2f, headerCellPaint); x += colBarcode
            canvas.drawText("Nama Obat & Sediaan", x, y + 2f, headerCellPaint); x += colName
            canvas.drawText("Golongan", x, y + 2f, headerCellPaint); x += colCat
            canvas.drawText("No. Batch", x, y + 2f, headerCellPaint); x += colBatch
            canvas.drawText("ED", x, y + 2f, headerCellPaint); x += colExp
            canvas.drawText("Gudang", x, y + 2f, headerCellPaint); x += colStock
            canvas.drawText("Sat", x, y + 2f, headerCellPaint); x += colUnit
            canvas.drawText("HPP (Rp)", x, y + 2f, headerCellPaint); x += colBuy
            canvas.drawText("Valuasi (Rp)", x, y + 2f, headerCellPaint)

            y += headerHeight

            // Draw Table Data Rows
            val startIdx = pageIndex * rowsPerPage
            val endIdx = (startIdx + rowsPerPage).coerceAtMost(warehouseItems.size)

            for (i in startIdx until endIdx) {
                val (drugItem, batch) = warehouseItems[i]
                val rowHeight = 17f

                if (i % 2 == 1) {
                    canvas.drawRect(
                        margin,
                        y - 9f,
                        pageWidth - margin,
                        y + rowHeight - 9f,
                        zebraBgPaint
                    )
                }

                x = margin + 4f
                canvas.drawText("${i + 1}", x, y + 2f, cellPaint); x += colNo
                canvas.drawText(drugItem.drug.barcode.take(11), x, y + 2f, cellPaint); x += colBarcode

                val truncatedName = if (drugItem.drug.name.length > 25) drugItem.drug.name.take(23) + ".." else drugItem.drug.name
                canvas.drawText(truncatedName, x, y + 2f, cellBoldPaint); x += colName

                val catShort = drugItem.drug.category.displayName.split(" ").firstOrNull() ?: "-"
                canvas.drawText(catShort, x, y + 2f, cellPaint); x += colCat

                canvas.drawText(batch.batchNumber.take(9), x, y + 2f, cellPaint); x += colBatch
                canvas.drawText(batch.expiryDate, x, y + 2f, cellPaint); x += colExp
                canvas.drawText("${batch.gudangStock}", x, y + 2f, cellBoldPaint); x += colStock
                canvas.drawText(drugItem.drug.unit.take(5), x, y + 2f, cellPaint); x += colUnit

                val hppStr = String.format(Locale("id", "ID"), "%,d", drugItem.drug.buyPrice.toInt())
                canvas.drawText(hppStr, x, y + 2f, cellPaint); x += colBuy

                val valStr = String.format(Locale("id", "ID"), "%,d", (drugItem.drug.buyPrice * batch.gudangStock).toInt())
                canvas.drawText(valStr, x, y + 2f, cellBoldPaint)

                canvas.drawLine(margin, y + rowHeight - 9f, pageWidth - margin, y + rowHeight - 9f, linePaint)
                y += rowHeight
            }

            // If last page: Draw Summary Box & Official Sign-off Signature block
            if (pageIndex == totalPages - 1) {
                y += 10f

                // Summary Card Box
                val summaryBoxPaint = Paint().apply {
                    color = Color.rgb(238, 244, 240)
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(
                    RectF(margin, y, pageWidth - margin, y + 36f),
                    4f,
                    4f,
                    summaryBoxPaint
                )
                val summaryBorderPaint = Paint().apply {
                    color = Color.rgb(180, 210, 190)
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                canvas.drawRoundRect(
                    RectF(margin, y, pageWidth - margin, y + 36f),
                    4f,
                    4f,
                    summaryBorderPaint
                )

                canvas.drawText(
                    "TOTAL REKAPITULASI INVENTARIS GUDANG: ${warehouseItems.size} SKU Batch Terdata",
                    margin + 8f,
                    y + 14f,
                    cellBoldPaint
                )
                canvas.drawText(
                    "Total Fisik Kuantitas: $totalUnits Unit  |  Estimasi Nilai Valuasi HPP Aset Gudang: ${formatRp(totalValuation)}",
                    margin + 8f,
                    y + 26f,
                    titlePaint
                )

                y += 50f

                // Lembar Pengesahan Tanda Tangan
                val sigDateStr = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
                val signTitlePaint = Paint().apply {
                    color = Color.rgb(27, 67, 50)
                    textSize = 8f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                canvas.drawText("Diverifikasi & Diserahkan:", margin + 20f, y, signTitlePaint)
                canvas.drawText("Disetujui & Divalidasi,", pageWidth - margin - 170f, y - 10f, metaPaint)
                canvas.drawText("Jakarta, $sigDateStr", pageWidth - margin - 170f, y, metaPaint)
                y += 11f
                canvas.drawText("Tenaga Teknis Kefarmasian (Gudang)", margin + 20f, y, metaPaint)
                canvas.drawText("Apoteker Pengelola Apotek (APA)", pageWidth - margin - 170f, y, signTitlePaint)

                y += 42f // Space for signature / stamp
                canvas.drawLine(margin + 20f, y, margin + 150f, y, linePaint)
                canvas.drawLine(pageWidth - margin - 170f, y, pageWidth - margin - 20f, y, linePaint)

                y += 10f
                canvas.drawText("( ${currentUser.fullName} )", margin + 20f, y, cellBoldPaint)
                canvas.drawText("( ${profile.apotekerName} )", pageWidth - margin - 170f, y, cellBoldPaint)
                y += 9f
                canvas.drawText("Petugas / Auditor Gudang", margin + 20f, y, metaPaint)
                canvas.drawText("SIPA: ${profile.sipaNumber}", pageWidth - margin - 170f, y, metaPaint)
            }

            // Footer (Page Number & Credential)
            val footerY = pageHeight - margin + 10f
            canvas.drawLine(margin, footerY - 12f, pageWidth - margin, footerY - 12f, linePaint)
            canvas.drawText(
                "Dokumen Sah Sistem Manajemen Farmasi Terpadu (FARMIVA Cloud) - Arsip Pelaporan Dinas Kesehatan & BPOM",
                margin,
                footerY,
                metaPaint
            )
            val pageStr = "Halaman ${pageIndex + 1} dari $totalPages"
            canvas.drawText(pageStr, pageWidth - margin - 70f, footerY, metaPaint)

            document.finishPage(page)
        }

        // Write document to cache directory
        val exportDir = File(context.cacheDir, "dinas_reports").apply { mkdirs() }
        val fileName = "Laporan_Dinas_Gudang_Farmiva_${System.currentTimeMillis()}.pdf"
        val pdfFile = File(exportDir, fileName)

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    /**
     * Generate an official, multi-page vector PDF for Stock Opname Discrepancy & Comparison Report.
     */
    fun generateStockOpnameComparisonPdf(
        context: Context,
        profile: PharmacyProfile,
        currentUser: UserAccount,
        items: List<OpnameDiscrepancyItem>,
        notes: String
    ): File {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 32f

        val titlePaint = Paint().apply {
            color = Color.rgb(27, 67, 50)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.rgb(45, 106, 79)
            textSize = 8.5f
            isAntiAlias = true
        }

        val metaPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 7.5f
            isAntiAlias = true
        }

        val headerCellPaint = Paint().apply {
            color = Color.WHITE
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val cellPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 7f
            isAntiAlias = true
        }

        val cellBoldPaint = Paint().apply {
            color = Color.rgb(27, 67, 50)
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val redPaint = Paint().apply {
            color = Color.rgb(220, 38, 38)
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bluePaint = Paint().apply {
            color = Color.rgb(37, 99, 235)
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val greenPaint = Paint().apply {
            color = Color.rgb(22, 101, 52)
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 0.8f
        }

        val headerBgPaint = Paint().apply {
            color = Color.rgb(45, 106, 79)
            style = Paint.Style.FILL
        }

        val zebraBgPaint = Paint().apply {
            color = Color.rgb(244, 247, 244)
            style = Paint.Style.FILL
        }

        val rowsPerPage = 23
        val totalPages = ((items.size + rowsPerPage - 1) / rowsPerPage).coerceAtLeast(1)
        val currentDateStr = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date())

        val totalLoss = items.sumOf { it.lossValue }
        val totalDiff = items.sumOf { it.diffUnits }

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            var y = margin + 12f

            // Top decorative bar
            val topBarPaint = Paint().apply {
                color = Color.rgb(45, 106, 79)
                style = Paint.Style.FILL
            }
            canvas.drawRect(margin, margin - 10f, pageWidth - margin, margin - 4f, topBarPaint)

            canvas.drawText("LAPORAN AUDIT & PEMBANDINGAN SELISIH STOCK OPNAME", margin, y, titlePaint)
            y += 12f
            canvas.drawText(
                "BERKAS REKONSILIASI FISIK VS BASIS DATA SISTEM APOTEK",
                margin,
                y,
                subTitlePaint
            )
            y += 11f
            canvas.drawText(
                "Apotek: ${profile.pharmacyName}  |  SIA: ${profile.siaNumber}  |  SIPA: ${profile.sipaNumber}",
                margin,
                y,
                metaPaint
            )
            y += 10f
            canvas.drawText(
                "Waktu Audit: $currentDateStr  |  Auditor: ${currentUser.fullName} (${currentUser.role.roleTitle})",
                margin,
                y,
                metaPaint
            )
            y += 10f
            canvas.drawText(
                "Catatan: $notes",
                margin,
                y,
                metaPaint
            )
            y += 8f

            val thickLinePaint = Paint().apply {
                color = Color.rgb(45, 106, 79)
                strokeWidth = 1.5f
            }
            canvas.drawLine(margin, y, pageWidth - margin, y, thickLinePaint)
            y += 12f

            // Columns (Sum = 531)
            val colNo = 18f
            val colBarcode = 55f
            val colName = 120f
            val colBatch = 48f
            val colExp = 42f
            val colSys = 40f
            val colPhys = 40f
            val colDiff = 40f
            val colVal = 55f
            val colStatus = 73f

            val headerHeight = 18f
            canvas.drawRoundRect(
                RectF(margin, y - 10f, pageWidth - margin, y + headerHeight - 10f),
                3f,
                3f,
                headerBgPaint
            )

            var x = margin + 3f
            canvas.drawText("No", x, y + 2f, headerCellPaint); x += colNo
            canvas.drawText("Barcode", x, y + 2f, headerCellPaint); x += colBarcode
            canvas.drawText("Nama Obat", x, y + 2f, headerCellPaint); x += colName
            canvas.drawText("Batch", x, y + 2f, headerCellPaint); x += colBatch
            canvas.drawText("ED", x, y + 2f, headerCellPaint); x += colExp
            canvas.drawText("Sist (Sblm)", x, y + 2f, headerCellPaint); x += colSys
            canvas.drawText("Fisik (Ssdh)", x, y + 2f, headerCellPaint); x += colPhys
            canvas.drawText("Selisih", x, y + 2f, headerCellPaint); x += colDiff
            canvas.drawText("Val. Kerugian", x, y + 2f, headerCellPaint); x += colVal
            canvas.drawText("Status & Alasan", x, y + 2f, headerCellPaint)

            y += headerHeight

            val startIdx = pageIndex * rowsPerPage
            val endIdx = (startIdx + rowsPerPage).coerceAtMost(items.size)

            for (i in startIdx until endIdx) {
                val item = items[i]
                val rowHeight = 17f

                if (i % 2 == 1) {
                    canvas.drawRect(
                        margin,
                        y - 9f,
                        pageWidth - margin,
                        y + rowHeight - 9f,
                        zebraBgPaint
                    )
                }

                x = margin + 3f
                canvas.drawText("${i + 1}", x, y + 2f, cellPaint); x += colNo
                canvas.drawText(item.drug.barcode.take(10), x, y + 2f, cellPaint); x += colBarcode

                val truncatedName = if (item.drug.name.length > 21) item.drug.name.take(19) + ".." else item.drug.name
                canvas.drawText(truncatedName, x, y + 2f, cellBoldPaint); x += colName

                canvas.drawText(item.batch.batchNumber.take(8), x, y + 2f, cellPaint); x += colBatch
                canvas.drawText(item.batch.expiryDate, x, y + 2f, cellPaint); x += colExp
                canvas.drawText("${item.systemTotal}", x, y + 2f, cellPaint); x += colSys
                canvas.drawText("${item.physicalTotal}", x, y + 2f, cellBoldPaint); x += colPhys

                val diffStr = "${if (item.diffUnits > 0) "+" else ""}${item.diffUnits}"
                val diffPaint = when {
                    item.diffUnits < 0 -> redPaint
                    item.diffUnits > 0 -> bluePaint
                    else -> greenPaint
                }
                canvas.drawText(diffStr, x, y + 2f, diffPaint); x += colDiff

                val lossStr = if (item.lossValue > 0) String.format(Locale("id", "ID"), "%,d", item.lossValue.toInt()) else "-"
                canvas.drawText(lossStr, x, y + 2f, if (item.lossValue > 0) redPaint else cellPaint); x += colVal

                val statusText = when {
                    item.diffUnits < 0 -> "KURANG"
                    item.diffUnits > 0 -> "LEBIH"
                    else -> "SESUAI"
                }
                val reasonText = item.manualAdjustment?.reason?.take(11) ?: ""
                val fullStatus = if (reasonText.isNotBlank()) "$statusText ($reasonText)" else statusText
                canvas.drawText(fullStatus.take(16), x, y + 2f, diffPaint)

                canvas.drawLine(margin, y + rowHeight - 9f, pageWidth - margin, y + rowHeight - 9f, linePaint)
                y += rowHeight
            }

            if (pageIndex == totalPages - 1) {
                y += 10f

                val summaryBoxPaint = Paint().apply {
                    color = Color.rgb(238, 244, 240)
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(RectF(margin, y, pageWidth - margin, y + 36f), 4f, 4f, summaryBoxPaint)

                canvas.drawText(
                    "HASIL AKHIR REKONSILIASI: ${items.size} SKU Terperiksa | Net Selisih: ${if (totalDiff > 0) "+" else ""}$totalDiff Unit",
                    margin + 8f,
                    y + 14f,
                    cellBoldPaint
                )
                canvas.drawText(
                    "Total Estimasi Kerugian Fisik HPP: ${formatRp(totalLoss)}  |  Status Audit: TEREKONSILIASI & SAH",
                    margin + 8f,
                    y + 26f,
                    if (totalLoss > 0) redPaint else greenPaint
                )

                y += 50f

                val sigDateStr = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
                val signTitlePaint = Paint().apply {
                    color = Color.rgb(27, 67, 50)
                    textSize = 8f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                canvas.drawText("Auditor Pelaksana,", margin + 20f, y, signTitlePaint)
                canvas.drawText("Mengetahui & Menyetujui,", pageWidth - margin - 170f, y - 10f, metaPaint)
                canvas.drawText("Jakarta, $sigDateStr", pageWidth - margin - 170f, y, metaPaint)
                y += 11f
                canvas.drawText("Petugas Stock Opname", margin + 20f, y, metaPaint)
                canvas.drawText("Apoteker Pengelola Apotek (APA)", pageWidth - margin - 170f, y, signTitlePaint)

                y += 42f
                canvas.drawLine(margin + 20f, y, margin + 150f, y, linePaint)
                canvas.drawLine(pageWidth - margin - 170f, y, pageWidth - margin - 20f, y, linePaint)

                y += 10f
                canvas.drawText("( ${currentUser.fullName} )", margin + 20f, y, cellBoldPaint)
                canvas.drawText("( ${profile.apotekerName} )", pageWidth - margin - 170f, y, cellBoldPaint)
                y += 9f
                canvas.drawText("Auditor Terverifikasi", margin + 20f, y, metaPaint)
                canvas.drawText("SIPA: ${profile.sipaNumber}", pageWidth - margin - 170f, y, metaPaint)
            }

            val footerY = pageHeight - margin + 10f
            canvas.drawLine(margin, footerY - 12f, pageWidth - margin, footerY - 12f, linePaint)
            canvas.drawText(
                "Dokumen Pembandingan Stock Opname Resmi FARMIVA - Standar Audit Farmasi",
                margin,
                footerY,
                metaPaint
            )
            val pageStr = "Halaman ${pageIndex + 1} dari $totalPages"
            canvas.drawText(pageStr, pageWidth - margin - 70f, footerY, metaPaint)

            document.finishPage(page)
        }

        val exportDir = File(context.cacheDir, "dinas_reports").apply { mkdirs() }
        val fileName = "Laporan_Perbandingan_Opname_${System.currentTimeMillis()}.pdf"
        val pdfFile = File(exportDir, fileName)

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    /**
     * Generate Excel XML (.xls) formatted for Dinas Kesehatan & BPOM inspection.
     */
    fun generateDinasWarehouseExcel(
        profile: PharmacyProfile,
        currentUser: UserAccount,
        warehouseItems: List<Pair<DrugWithBatches, StockBatch>>
    ): String {
        val totalUnits = warehouseItems.sumOf { it.second.gudangStock }
        val totalValuation = warehouseItems.sumOf { it.first.drug.buyPrice * it.second.gudangStock }
        val dateStr = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date())

        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<?mso-application progid="Excel.Sheet"?>""")
            appendLine("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"""")
            appendLine(""" xmlns:o="urn:schemas-microsoft-com:office:office"""")
            appendLine(""" xmlns:x="urn:schemas-microsoft-com:office:excel"""")
            appendLine(""" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"""")
            appendLine(""" xmlns:html="http://www.w3.org/TR/REC-html40">""")
            appendLine(""" <Styles>""")
            appendLine("""  <Style ss:ID="Header">""")
            appendLine("""   <Font ss:Bold="1" ss:Color="#FFFFFF" ss:FontName="Calibri" ss:Size="11"/>""")
            appendLine("""   <Interior ss:Color="#1B4332" ss:Pattern="Solid"/>""")
            appendLine("""   <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>""")
            appendLine("""  </Style>""")
            appendLine("""  <Style ss:ID="Title">""")
            appendLine("""   <Font ss:Bold="1" ss:Size="14" ss:Color="#1B4332" ss:FontName="Calibri"/>""")
            appendLine("""  </Style>""")
            appendLine("""  <Style ss:ID="SubTitle">""")
            appendLine("""   <Font ss:Bold="1" ss:Size="11" ss:Color="#2D6A4F" ss:FontName="Calibri"/>""")
            appendLine("""  </Style>""")
            appendLine("""  <Style ss:ID="Meta">""")
            appendLine("""   <Font ss:Size="9" ss:Color="#475569" ss:FontName="Calibri"/>""")
            appendLine("""  </Style>""")
            appendLine("""  <Style ss:ID="Currency">""")
            appendLine("""   <NumberFormat ss:Format="#,##0"/>""")
            appendLine("""   <Alignment ss:Horizontal="Right"/>""")
            appendLine("""  </Style>""")
            appendLine("""  <Style ss:ID="TotalRow">""")
            appendLine("""   <Font ss:Bold="1" ss:Color="#1B4332" ss:FontName="Calibri"/>""")
            appendLine("""   <Interior ss:Color="#E8F5E9" ss:Pattern="Solid"/>""")
            appendLine("""  </Style>""")
            appendLine(""" </Styles>""")
            appendLine(""" <Worksheet ss:Name="Laporan Gudang Dinas">""")
            appendLine("""  <Table>""")
            appendLine("""   <Column ss:Width="40"/>""")
            appendLine("""   <Column ss:Width="100"/>""")
            appendLine("""   <Column ss:Width="180"/>""")
            appendLine("""   <Column ss:Width="110"/>""")
            appendLine("""   <Column ss:Width="80"/>""")
            appendLine("""   <Column ss:Width="75"/>""")
            appendLine("""   <Column ss:Width="70"/>""")
            appendLine("""   <Column ss:Width="50"/>""")
            appendLine("""   <Column ss:Width="80"/>""")
            appendLine("""   <Column ss:Width="95"/>""")
            appendLine("""   <Column ss:Width="130"/>""")
            appendLine("""   <Column ss:Width="70"/>""")

            // Official Kop & Titles
            appendLine("""   <Row><Cell ss:StyleID="Title"><Data ss:Type="String">BERKAS RESMI PELAPORAN INVENTARISASI GUDANG FARMASI</Data></Cell></Row>""")
            appendLine("""   <Row><Cell ss:StyleID="SubTitle"><Data ss:Type="String">DINAS KESEHATAN KOTA &amp; BALAI BESAR POM (BPOM)</Data></Cell></Row>""")
            appendLine("""   <Row><Cell ss:StyleID="Meta"><Data ss:Type="String">Sarana: ${profile.pharmacyName} | No. SIA: ${profile.siaNumber} | No. SIPA: ${profile.sipaNumber}</Data></Cell></Row>""")
            appendLine("""   <Row><Cell ss:StyleID="Meta"><Data ss:Type="String">Alamat: ${profile.address} | Telp: ${profile.phone} | APA: ${profile.apotekerName}</Data></Cell></Row>""")
            appendLine("""   <Row><Cell ss:StyleID="Meta"><Data ss:Type="String">Tanggal Ekspor: $dateStr | Pemeriksa: ${currentUser.fullName} (${currentUser.role.roleTitle})</Data></Cell></Row>""")
            appendLine("""   <Row><Cell ss:StyleID="Meta"><Data ss:Type="String">Rekapitulasi: ${warehouseItems.size} Batch SKU | Total Unit Fisik: $totalUnits | Valuasi HPP: ${formatRp(totalValuation)}</Data></Cell></Row>""")
            appendLine("""   <Row/>""")

            // Table Header
            appendLine("""   <Row ss:StyleID="Header" ss:Height="22">""")
            listOf(
                "No", "Barcode", "Nama Obat & Bentuk Sediaan", "Golongan Kategori",
                "No. Batch", "Kadaluwarsa (ED)", "Stok Gudang", "Satuan",
                "Harga Beli (HPP)", "Total Valuasi (Rp)", "PBF / Distributor", "Kondisi Fisik"
            ).forEach { col ->
                appendLine("""    <Cell><Data ss:Type="String">$col</Data></Cell>""")
            }
            appendLine("""   </Row>""")

            // Rows
            warehouseItems.forEachIndexed { idx, (drugItem, batch) ->
                val rowValuation = drugItem.drug.buyPrice * batch.gudangStock
                val safeName = drugItem.drug.name.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                val safeSupplier = batch.supplierName.replace("&", "&amp;")

                appendLine("""   <Row>""")
                appendLine("""    <Cell><Data ss:Type="Number">${idx + 1}</Data></Cell>""")
                appendLine("""    <Cell><Data ss:Type="String">${drugItem.drug.barcode}</Data></Cell>""")
                appendLine("""    <Cell><Data ss:Type="String">$safeName</Data></Cell>""")
                appendLine("""    <Cell><Data ss:Type="String">${drugItem.drug.category.displayName}</Data></Cell>""")
                appendLine("""    <Cell><Data ss:Type="String">${batch.batchNumber}</Data></Cell>""")
                appendLine("""    <Cell><Data ss:Type="String">${batch.expiryDate}</Data></Cell>""")
                appendLine("""    <Cell><Data ss:Type="Number">${batch.gudangStock}</Data></Cell>""")
                appendLine("""    <Cell><Data ss:Type="String">${drugItem.drug.unit}</Data></Cell>""")
                appendLine("""    <Cell ss:StyleID="Currency"><Data ss:Type="Number">${drugItem.drug.buyPrice.toInt()}</Data></Cell>""")
                appendLine("""    <Cell ss:StyleID="Currency"><Data ss:Type="Number">${rowValuation.toInt()}</Data></Cell>""")
                appendLine("""    <Cell><Data ss:Type="String">$safeSupplier</Data></Cell>""")
                appendLine("""    <Cell><Data ss:Type="String">Baik</Data></Cell>""")
                appendLine("""   </Row>""")
            }

            // Total Row
            appendLine("""   <Row ss:StyleID="TotalRow" ss:Height="20">""")
            appendLine("""    <Cell><Data ss:Type="String">TOTAL</Data></Cell>""")
            appendLine("""    <Cell/>""")
            appendLine("""    <Cell><Data ss:Type="String">${warehouseItems.size} SKU Batch Terdata</Data></Cell>""")
            appendLine("""    <Cell/>""")
            appendLine("""    <Cell/>""")
            appendLine("""    <Cell/>""")
            appendLine("""    <Cell><Data ss:Type="Number">$totalUnits</Data></Cell>""")
            appendLine("""    <Cell><Data ss:Type="String">Unit</Data></Cell>""")
            appendLine("""    <Cell/>""")
            appendLine("""    <Cell ss:StyleID="Currency"><Data ss:Type="Number">${totalValuation.toInt()}</Data></Cell>""")
            appendLine("""    <Cell/>""")
            appendLine("""    <Cell/>""")
            appendLine("""   </Row>""")

            appendLine("""   <Row/>""")
            appendLine("""   <Row><Cell><Data ss:Type="String">Lembar Pengesahan Laporan:</Data></Cell></Row>""")
            appendLine("""   <Row><Cell><Data ss:Type="String">Tenaga Teknis Kefarmasian Gudang: ${currentUser.fullName}</Data></Cell><Cell/><Cell/><Cell/><Cell/><Cell><Data ss:Type="String">Apoteker Pengelola Apotek: ${profile.apotekerName} (${profile.sipaNumber})</Data></Cell></Row>""")

            appendLine("""  </Table>""")
            appendLine(""" </Worksheet>""")
            appendLine("""</Workbook>""")
        }
    }
}
