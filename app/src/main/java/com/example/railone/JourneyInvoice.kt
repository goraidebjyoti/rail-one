package com.example.railone

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File

// A dedicated provider avoids direct FileProvider instantiation quirks on vendor devices.
class JourneyInvoiceProvider : FileProvider()

internal const val INVOICE_NOTICE = "This invoice is for reference only and is not valid as a travel ticket."
internal fun invoiceFileName(data: TicketData): String {
    val id = data.journeyTicket.replace(Regex("[^A-Za-z0-9_-]"), "_").take(80).ifBlank { "ticket" }
    return "${id}_journey_invoice.pdf"
}

internal fun createJourneyInvoice(context: Context, data: TicketData): File {
    val directory = File(context.cacheDir, "invoices").apply { check(isDirectory || mkdirs()) }
    val file = File(directory, invoiceFileName(data))
    val temporary = File.createTempFile("invoice-", ".tmp", directory)
    val pdf = PdfDocument()
    try {
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        drawJourneyInvoice(context, page.canvas, data)
        pdf.finishPage(page)
        temporary.outputStream().use { pdf.writeTo(it) }
        check(temporary.renameTo(file)) { "Could not save invoice" }
    } finally { pdf.close(); temporary.delete() }
    return file
}

internal fun journeyInvoiceShareIntent(context: Context, file: File): Intent {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.invoices", file)
    return Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, file.name)
        clipData = ClipData.newUri(context.contentResolver, file.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun drawJourneyInvoice(context: Context, canvas: Canvas, data: TicketData) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    fun text(value: String, x: Float, y: Float, size: Float = 11f, bold: Boolean = false,
        colour: Int = Color.BLACK, width: Float = 510f, align: Paint.Align = Paint.Align.LEFT) {
        paint.color = colour; paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        paint.textAlign = align; paint.textSize = size
        // Station names, customer names and Via stay readable without spilling into other columns.
        val measured = paint.measureText(value)
        if (measured > width) paint.textSize = size * width / measured
        canvas.drawText(value, x, y, paint)
    }
    fun line(y: Float) {
        paint.color = Color.BLACK; paint.strokeWidth = .8f
        canvas.drawLine(42f, y, 553f, y, paint)
    }
    fun panel(bounds: RectF) {
        paint.style = Paint.Style.FILL; paint.color = Color.rgb(250, 250, 250)
        canvas.drawRoundRect(bounds, 8f, 8f, paint)
        paint.style = Paint.Style.STROKE; paint.color = Color.rgb(218, 218, 218); paint.strokeWidth = .8f
        canvas.drawRoundRect(bounds, 8f, 8f, paint); paint.style = Paint.Style.FILL
    }
    canvas.drawColor(Color.WHITE)
    val watermark = BitmapFactory.decodeResource(context.resources, R.drawable.invoice_watermark)
    val mark = BitmapFactory.decodeResource(context.resources, R.drawable.app_icon)
    val railways = BitmapFactory.decodeResource(context.resources, R.drawable.invoice_railways)
    try {
        val saved = canvas.save()
        canvas.clipRect(24f, 24f, 571f, 818f)
        val faint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 20; isFilterBitmap = true }
        for (row in 0..5) for (col in 0..3) {
            val x = col * 160f - 55f; val y = row * 155f + 25f
            val stamp = canvas.save()
            canvas.rotate(30f, x + 80f, y + 55f)
            canvas.drawBitmap(watermark, null, RectF(x, y, x + 160f, y + 107f), faint)
            canvas.restoreToCount(stamp)
        }
        canvas.restoreToCount(saved)
        paint.style = Paint.Style.STROKE; paint.color = Color.rgb(218, 218, 218); paint.strokeWidth = .8f
        canvas.drawRoundRect(RectF(24f, 24f, 571f, 818f), 10f, 10f, paint)
        paint.style = Paint.Style.FILL
        canvas.drawBitmap(mark, null, RectF(42f, 42f, 102f, 102f), Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true })
        canvas.drawBitmap(railways, null, RectF(493f, 42f, 553f, 102f), Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true })
        text("UTS Invoice", 130f, 80f, 22f, true)
        line(120f)
        panel(RectF(42f, 138f, 553f, 212f))
        text("Customer Details", 52f, 161f, 12f, true)
        text("Passenger Name", 52f, 182f, 9f, true, Color.DKGRAY)
        text(data.passengerName, 52f, 198f, 11f, true, width = 330f)
        text("Mobile Number", 543f, 182f, 9f, true, Color.DKGRAY, align = Paint.Align.RIGHT)
        text(data.mobile, 543f, 198f, 11f, true, width = 130f, align = Paint.Align.RIGHT)
        text("Journey Ticket", 42f, 244f, 12f, true)
        text(data.journeyTicket, 553f, 244f, 12f, true, width = 240f, align = Paint.Align.RIGHT)
        text(data.origin, 42f, 275f, 12f, true, width = 205f)
        text("${normalDistance(data.distance)} km", 297.5f, 277f, 10f, colour = Color.DKGRAY, width = 70f, align = Paint.Align.CENTER)
        text(data.destination, 553f, 275f, 12f, true, width = 205f, align = Paint.Align.RIGHT)
        text("Via", 42f, 306f, 10f, true, Color.DKGRAY)
        text(data.via.ifBlank { "-" }, 42f, 323f, 11f, true, width = 340f)
        text("Passenger", 553f, 306f, 10f, true, Color.DKGRAY, align = Paint.Align.RIGHT)
        text(invoicePassengers(data), 553f, 323f, 11f, true, width = 150f, align = Paint.Align.RIGHT)
        text("Booked On", 42f, 349f, 10f, true, Color.DKGRAY)
        text(invoiceBookingTime(data.bookedOn), 42f, 366f, 11f, true)
        line(390f)
        paint.color = Color.rgb(243, 243, 243)
        val summary = invoiceSummary(data)
        paint.textSize = 14f; paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        canvas.drawRoundRect(RectF(42f, 410f, (62f + paint.measureText(summary)).coerceAtMost(553f), 448f), 7f, 7f, paint)
        text(summary, 52f, 432f, 14f, true, width = 491f)
        text(INVOICE_NOTICE, 297.5f, 479f, 16f, colour = Color.rgb(255, 65, 52), align = Paint.Align.CENTER)
        line(770f)
        text("This invoice is computer-generated. No signature is required.", 297.5f, 796f, 12f,
            colour = Color.DKGRAY, align = Paint.Align.CENTER)
    } finally { watermark.recycle(); mark.recycle(); railways.recycle() }
}
