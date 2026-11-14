package ge.garantigroup.framestudio

import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class DrawingBox(val x: Float, val y: Float, val w: Float, val h: Float) {
    fun contains(px: Float, py: Float) = px >= x && px <= x + w && py >= y && py <= y + h
}
data class DrawingChip(val box: DrawingBox, val hit: DimensionHit)
data class DrawingGeometry(val box: DrawingBox, val chips: List<DrawingChip>)

fun geometryFor(s: FrameConstruction): DrawingGeometry {
    val scale = min(560f / max(200, s.width), 425f / max(200, s.height))
    val w = s.width * scale
    val h = s.height * scale
    val box = DrawingBox(120f + (560 - w) / 2, 75f + (425 - h) / 2, w, h)
    val chips = mutableListOf<DrawingChip>()
    fun chip(x: Float, y: Float, ww: Float, hh: Float, hit: DimensionHit) {
        chips += DrawingChip(DrawingBox(x, y, ww, hh), hit)
    }
    chip(box.x + box.w / 2 - 65, 12f, 130f, 36f,
        DimensionHit("width", old = s.width))
    chip(4f, box.y + box.h / 2 - 18, 76f, 36f,
        DimensionHit("height", old = s.height))
    val cuts = s.cells.flatMap { listOf(it.x, it.x + it.w) }
        .filter { it > 0.000001 && it < 0.999999 }.distinct()
        .sorted()
    val edges = listOf(0.0) + cuts + 1.0
    for (i in 0 until edges.size - 1) {
        val st = edges[i]; val en = edges[i + 1]
        chip(box.x + ((st + en) / 2 * box.w).toFloat() - 34, box.y - 78, 68f, 29f,
            DimensionHit("segment", st, en, old = ((en - st) * s.width).roundToInt()))
    }
    for (side in listOf("left", "right")) {
        val group = s.cells.filter {
            if (side == "left") abs(it.x) < 0.00001 else abs(it.x + it.w - 1) < 0.00001
        }
        val yc = group.flatMap { listOf(it.y, it.y + it.h) }
            .filter { it > 0.000001 && it < 0.999999 }.distinct().sorted()
        val ye = listOf(0.0) + yc + 1.0
        for (i in 0 until ye.size - 1) {
            val st = ye[i]; val en = ye[i + 1]
            val x = if (side == "left") max(2f, box.x - 90) else min(718f, box.x + box.w + 10)
            chip(x, box.y + ((st + en) / 2 * box.h).toFloat() - 15, 72f, 30f,
                DimensionHit("vsegment", st, en, side, ((en - st) * s.height).roundToInt()))
        }
    }
    return DrawingGeometry(box, chips)
}

private fun parse(color: String): Int = AndroidColor.parseColor(color)
private fun paint(color: String, size: Float = 1f, stroke: Boolean = false): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = parse(color)
        this.strokeWidth = size
        this.style = if (stroke) Paint.Style.STROKE else Paint.Style.FILL
    }
private fun textPaint(color: String, size: Float, bold: Boolean = false): Paint =
    paint(color).apply {
        textSize = size
        textAlign = Paint.Align.CENTER
        typeface = if (bold) android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        else android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
    }
private fun frameColor(name: String): String = when (name) {
    "თეთრი" -> "#F7F7F5"; "შავი მატი" -> "#202124"
    "შავი NOIR 2100" -> "#17191C"; "ანტრაციტი RAL 7016" -> "#383E42"
    "ნაცრისფერი RAL 7004" -> "#9B9D9A"; "ყავისფერი RAL 8017" -> "#44312B"
    "ვერცხლისფერი" -> "#C0C4C7"; "ოქროსფერი" -> "#C5A16C"
    "ხის ფაქტურა" -> "#94623D"; else -> "#263E5A"
}

fun drawFrameOnCanvas(
    canvas: AndroidCanvas,
    frame: FrameConstruction,
    selectedId: String? = null,
    editing: Boolean = true
) {
    val g = geometryFor(frame)
    val b = g.box
    canvas.drawColor(if (editing) parse("#eef3fa") else AndroidColor.WHITE)
    val border = paint("#9ca8b8", 1.3f, true)
    canvas.drawLine(b.x,b.y-26,b.x+b.w,b.y-26,border)
    canvas.drawLine(b.x,b.y-33,b.x,b.y-18,border)
    canvas.drawLine(b.x+b.w,b.y-33,b.x+b.w,b.y-18,border)
    canvas.drawLine(b.x-28,b.y,b.x-28,b.y+b.h,border)
    canvas.drawLine(b.x-35,b.y,b.x-20,b.y,border)
    canvas.drawLine(b.x-35,b.y+b.h,b.x-20,b.y+b.h,border)
    val frameWidth = min(11f, max(4f, min(560f/frame.width, 425f/frame.height)*50))
    val openingLine = paint("#3576a8",2.2f,true).apply { pathEffect = DashPathEffect(floatArrayOf(6f,4f),0f) }
    frame.cells.forEach { cell ->
        val x = b.x + (cell.x*b.w).toFloat()
        val y = b.y + (cell.y*b.h).toFloat()
        val w = (cell.w*b.w).toFloat()
        val h = (cell.h*b.h).toFloat()
        val selected = editing && cell.id == selectedId
        canvas.drawRect(x,y,x+w,y+h,paint(if (selected) "#cfe8ff" else "#d8edfa"))
        canvas.drawRect(x,y,x+w,y+h,paint(frameColor(frame.color),frameWidth,true))
        canvas.drawRect(x+9,y+9,x+w-9,y+h-9,paint(if (selected) "#d8efff" else "#eff9ff"))
        canvas.drawRect(x+9,y+9,x+w-9,y+h-9,paint("#8ca6bf",1.5f,true))
        val cx=x+w/2; val cy=y+h/2
        if (cell.opening!="fixed") {
            val desc=if (cell.direction=="out") "↗ გარეთ" else "↙ შიგნით"
            canvas.drawText(desc,x+w-55,y+22,textPaint("#1762b3",13f))
            val inset = min(18f,min(w/6,h/6))
            val left=x+inset; val right=x+w-inset
            val top=y+inset; val bottom=y+h-inset
            if (cell.opening=="left" || cell.opening=="tiltleft") {
                canvas.drawLine(left,top,right,cy,openingLine)
                canvas.drawLine(right,cy,left,bottom,openingLine)
            }
            if (cell.opening=="right" || cell.opening=="tiltright") {
                canvas.drawLine(right,top,left,cy,openingLine)
                canvas.drawLine(left,cy,right,bottom,openingLine)
            }
            if (cell.opening.startsWith("tilt")) {
                canvas.drawLine(left,bottom,cx,top,openingLine)
                canvas.drawLine(cx,top,right,bottom,openingLine)
            }
        }
        if (w>70 && h>55) {
            val msg=(cell.w*frame.width).roundToInt().toString() + " × " +
                (cell.h*frame.height).roundToInt().toString()
            canvas.drawText(msg,cx,cy+6,textPaint("#35516c",min(15f,min(w/6,h/5))))
        }
    }
    if (editing) {
        for (chip in g.chips) {
            val r=chip.box
            canvas.drawRoundRect(r.x,r.y,r.x+r.w,r.y+r.h,9f,9f,paint("#dceaff"))
            canvas.drawRoundRect(r.x,r.y,r.x+r.w,r.y+r.h,9f,9f,paint("#377cf3",1.6f,true))
            canvas.drawText(chip.hit.old.toString(),r.x+r.w/2,r.y+r.h/2+6,textPaint("#095ac4",19f,true))
        }
    } else {
        canvas.drawText(frame.width.toString()+" მმ",b.x+b.w/2,b.y-39,textPaint("#1c3553",18f,true))
        canvas.save()
        canvas.rotate(-90f,b.x-44,b.y+b.h/2)
        canvas.drawText(frame.height.toString()+" მმ",b.x-44,b.y+b.h/2,textPaint("#1c3553",18f,true))
        canvas.restore()
    }
}

@Composable
fun FrameDrawing(
    frame: FrameConstruction,
    selectedId: String?,
    tool: String,
    onCellTap: (String) -> Unit,
    onCellLongPress: (String) -> Unit,
    onDimension: (DimensionHit) -> Unit,
    onImpostDragStart: () -> Unit,
    onImpostDrag: (ImpostBoundary, Double) -> Double?
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val geo = geometryFor(frame)
    val latestFrame by rememberUpdatedState(frame)
    val latestDragStart by rememberUpdatedState(onImpostDragStart)
    val latestDrag by rememberUpdatedState(onImpostDrag)
    Column(Modifier.fillMaxWidth().background(Color(0xFFEEF3FA), RoundedCornerShape(14.dp)).padding(8.dp)) {
        Canvas(
            Modifier.fillMaxWidth().heightIn(min=310.dp).height(340.dp)
                .pointerInput(frame, selectedId, tool, zoom, pan) {
                    fun virtual(pos: Offset): Offset {
                        val px = pos.x / size.width * 800f
                        val py = pos.y / size.height * 600f
                        val center = Offset(400f,300f)
                        return (Offset(px,py) - center) / zoom + center - pan
                    }
                    detectTapGestures(
                        onTap = { p ->
                            val v=virtual(p)
                            val hit=geo.chips.lastOrNull { it.box.contains(v.x,v.y) }
                            if (hit!=null) onDimension(hit.hit)
                            else {
                                val b=geo.box
                                if (b.contains(v.x,v.y)) {
                                    val cx=(v.x-b.x)/b.w
                                    val cy=(v.y-b.y)/b.h
                                    val c=frame.cells.lastOrNull { cell ->
                                        cx>=cell.x && cx<=cell.x+cell.w &&
                                            cy>=cell.y && cy<=cell.y+cell.h
                                    }
                                    if (c!=null) onCellTap(c.id)
                                }
                            }
                        },
                        onLongPress = { p ->
                            val v=virtual(p)
                            val b=geo.box
                            if (b.contains(v.x,v.y)) {
                                val x=(v.x-b.x)/b.w
                                val y=(v.y-b.y)/b.h
                                frame.cells.lastOrNull { x>=it.x && x<=it.x+it.w &&
                                    y>=it.y && y<=it.y+it.h }?.let { onCellLongPress(it.id) }
                            }
                        }
                    )
                }
                .pointerInput(tool) {
                    var active: ImpostBoundary? = null
                    fun virtual(pos: Offset): Offset {
                        val px=pos.x / size.width * 800f
                        val py=pos.y / size.height * 600f
                        val center=Offset(400f,300f)
                        return (Offset(px,py)-center)/zoom+center-pan
                    }
                    detectDragGestures(
                        onDragStart = { p ->
                            active=null
                            if (tool=="select") {
                                val b=geometryFor(latestFrame).box
                                val v=virtual(p)
                                active=impostBoundaries(latestFrame).firstOrNull { d ->
                                    if(d.axis=="vertical") {
                                        val lineX=b.x+(d.pos*b.w).toFloat()
                                        val top=b.y+(d.lo*b.h).toFloat()
                                        val bottom=b.y+(d.hi*b.h).toFloat()
                                        kotlin.math.abs(v.x-lineX)<17f && v.y in top..bottom
                                    } else {
                                        val lineY=b.y+(d.pos*b.h).toFloat()
                                        val left=b.x+(d.lo*b.w).toFloat()
                                        val right=b.x+(d.hi*b.w).toFloat()
                                        kotlin.math.abs(v.y-lineY)<17f && v.x in left..right
                                    }
                                }
                                if(active!=null)latestDragStart()
                            }
                        },
                        onDrag = { change, drag ->
                            val d=active
                            if(d!=null) {
                                change.consume()
                                val b=geometryFor(latestFrame).box
                                val displacement=if(d.axis=="vertical") {
                                    drag.x / size.width * 800f / zoom / b.w
                                } else {
                                    drag.y / size.height * 600f / zoom / b.h
                                }
                                val newPos=latestDrag(d,d.pos+displacement)
                                if(newPos!=null)active=d.copy(pos=newPos)
                            }
                        },
                        onDragCancel = { active=null },
                        onDragEnd = { active=null }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, drag, amount, _ ->
                        zoom = (zoom*amount).coerceIn(1f,4f)
                        if (zoom>1f) pan += Offset(drag.x / size.width * 800f, drag.y / size.height * 600f)
                        else pan=Offset.Zero
                    }
                }
        ) {
            drawIntoCanvas { draw ->
                val native=draw.nativeCanvas
                native.save()
                native.scale(size.width/800f,size.height/600f)
                native.translate(400f,300f)
                native.scale(zoom,zoom)
                native.translate(-400f+pan.x,-300f+pan.y)
                drawFrameOnCanvas(native,frame,selectedId,editing=true)
                native.restore()
            }
        }
    }
}
