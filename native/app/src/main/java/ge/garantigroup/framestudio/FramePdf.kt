package ge.garantigroup.framestudio

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object FramePdf {
    private fun pen(size: Float, bold: Boolean = false, color: Int = Color.BLACK): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    fun generate(context: Context, order: FrameOrder): File {
        val pdf = PdfDocument()
        order.constructions.forEachIndexed { index, frame ->
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, index + 1).create())
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)
            val dark = Color.rgb(29, 42, 64)
            canvas.drawText("გარანტი ჯგუფი", 225f, 40f, pen(20f, true, dark))
            canvas.drawText(order.number.ifBlank { "ახალი შეკვეთა" }, 435f, 59f, pen(11f, true))
            canvas.drawText(order.date, 435f, 75f, pen(9f))
            canvas.drawRect(30f, 87f, 565f, 89f, pen(1f, color = dark))
            var y = 110f
            if (index == 0) {
                val fields = listOf(
                    "შეკვეთის №" to order.number,
                    "თარიღი" to order.date,
                    "დამკვეთი" to order.customer,
                    "ტელეფონი" to order.phone,
                    "მისამართი / ობიექტი" to order.address,
                    "რაოდენობა" to order.quantity
                )
                fields.forEachIndexed { i, (name, value) ->
                    val col = i % 2
                    val row = i / 2
                    val x = 32f + col * 268
                    val yy = 106f + row * 35
                    canvas.drawText(name, x, yy, pen(8f, color = Color.DKGRAY))
                    canvas.drawText(value.ifBlank { "—" }.take(40), x, yy + 16, pen(11f, true))
                    canvas.drawLine(x, yy+22, x+250, yy+22, pen(0.7f,color=Color.LTGRAY))
                }
                y = 232f
            }
            canvas.drawText(
                (index + 1).toString() + ". " + frame.kind + " — " +
                    frame.width + " × " + frame.height + " მმ",
                32f, y, pen(14f, true, dark)
            )
            y += 18f
            listOf(
                "პროფილი: " + frame.profile.ifBlank { "—" },
                "ფერი: " + frame.color.ifBlank { "—" },
                "მინაპაკეტი: " + frame.glass.ifBlank { "—" }
            ).forEach {
                canvas.drawText(it.take(75), 32f, y, pen(10f))
                y += 15f
            }
            val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
            val drawingCanvas = Canvas(bitmap)
            drawFrameOnCanvas(drawingCanvas, frame, editing = false)
            val drawY = y+6f
            canvas.drawBitmap(bitmap, null, RectF(32f,drawY,563f,drawY+398f), Paint(Paint.FILTER_BITMAP_FLAG))
            bitmap.recycle()
            canvas.drawText("სქემატური ნახაზი · ზომები მილიმეტრებში", 165f, drawY+415, pen(9f, color = Color.DKGRAY))
            if (frame.notes.isNotBlank()) {
                canvas.drawText("შენიშვნები:",32f,drawY+448,pen(10f,true))
                frame.notes.lines().take(3).forEachIndexed { line, str ->
                    canvas.drawText(str.take(85),32f,drawY+464+line*15,pen(9f))
                }
            }
            pdf.finishPage(page)
        }
        val directory = File(context.cacheDir,"shared").apply { mkdirs() }
        val file = File(directory,"FRAME-STUDIO-" + order.number.ifBlank { "ახალი" } + ".pdf")
        try { FileOutputStream(file).use { pdf.writeTo(it) } }
        finally { pdf.close() }
        return file
    }

    fun shareOnWhatsApp(context: Context, order: FrameOrder) {
        val file = generate(context,order)
        val uri = FileProvider.getUriForFile(context,
            context.packageName + ".fileprovider",file)
        val base = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM,uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        for (app in listOf("com.whatsapp","com.whatsapp.w4b")) {
            try {
                context.startActivity(Intent(base).setPackage(app))
                return
            } catch (_: android.content.ActivityNotFoundException) {}
        }
        context.startActivity(Intent.createChooser(base,"PDF-ის გაზიარება"))
    }
}
