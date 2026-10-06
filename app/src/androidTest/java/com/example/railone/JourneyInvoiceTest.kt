package com.example.railone

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class JourneyInvoiceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun ticket() = freshDraft().copy(passengerName = "Debjyoti Gorai", mobile = "7431922555",
        origin = "KHARAGPUR", destination = "HOWRAH", distance = "116 km", via = "",
        journeyTicket = "XI4EE2N06F", bookedOn = "09/05/2025 18:31", fare = "60.00",
        className = "SECOND", trainType = "MAIL/EXPRESS", ticketType = "JOURNEY", adults = "1", children = "0")
    @Test fun invoiceUsesSavedTicketValuesAndSafeReferenceFilename() {
        val data = ticket()
        assertEquals("XI4EE2N06F_journey_invoice.pdf", invoiceFileName(data))
        assertEquals("2025-05-09 18:31:00", invoiceBookingTime(data.bookedOn))
        assertEquals("1 Adult, 0 Child", invoicePassengers(data))
        assertEquals("SECOND | MAIL/EXPRESS | JOURNEY | ₹60.00", invoiceSummary(data))
        assertFalse(invoiceFileName(data.copy(journeyTicket = "../../ticket")).contains('/'))
    }
    @Test fun invoiceIsReadableOnePageA4PdfWithVisibleInvalidTravelNotice() {
        val file = createJourneyInvoice(context, ticket())
        assertTrue(file.length() > 1000)
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                assertEquals(1, renderer.pageCount)
                renderer.openPage(0).use { page ->
                    assertEquals(595, page.width); assertEquals(842, page.height)
                    val bitmap = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
                    try {
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        var redPixels = 0
                        for (y in 458..483) for (x in 40..554) {
                            val colour = bitmap.getPixel(x, y)
                            if (Color.red(colour) > 180 && Color.green(colour) < 130 && Color.blue(colour) < 130) redPixels++
                        }
                        assertTrue("Travel-invalid notice should be visible in red", redPixels > 100)
                    } finally { bitmap.recycle() }
                }
            }
        }
        file.delete()
    }
    @Test fun shareIntentProvidesOnlyPdfContentUriWithReadPermission() {
        val file = createJourneyInvoice(context, ticket())
        val intent = journeyInvoiceShareIntent(context, file)
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("application/pdf", intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        val uri = intent.clipData!!.getItemAt(0).uri
        assertEquals("content", uri.scheme)
        assertEquals("${context.packageName}.invoices", uri.authority)
        assertEquals(file.name, intent.getStringExtra(Intent.EXTRA_TITLE))
        assertArrayEquals(file.readBytes(), context.contentResolver.openInputStream(uri)!!.use { it.readBytes() })
        file.delete()
    }
    @Test fun fileProviderDoesNotExposeProfilePreferencesOrOtherCacheFiles() {
        val outside = File(context.cacheDir, "private-test.txt").apply { writeText("private") }
        try {
            assertTrue(runCatching {
                androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.invoices", outside)
            }.isFailure)
        } finally { outside.delete() }
    }
}
