package ge.garantigroup.framestudio

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun newId(): String = UUID.randomUUID().toString()
fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

data class FrameCell(
    val id: String = newId(),
    val x: Double = 0.0,
    val y: Double = 0.0,
    val w: Double = 1.0,
    val h: Double = 1.0,
    val opening: String = "fixed",
    val direction: String = "in"
)
data class FrameConstruction(
    val id: String = newId(),
    val kind: String = "ფანჯარა",
    val profile: String = "",
    val color: String = "",
    val glass: String = "",
    val notes: String = "",
    val width: Int = 1800,
    val height: Int = 1500,
    val cells: List<FrameCell> = listOf(FrameCell())
)
data class FrameOrder(
    val id: String = newId(),
    val number: String = "",
    val date: String = today(),
    val customer: String = "",
    val phone: String = "",
    val quantity: String = "1",
    val address: String = "",
    val constructions: List<FrameConstruction> = listOf(FrameConstruction())
)

val openingNames = linkedMapOf(
    "fixed" to "ფიქსირებული",
    "left" to "მარცხენა გაღება",
    "right" to "მარჯვენა გაღება",
    "tilt" to "გადმოსაწევი",
    "tiltleft" to "მარცხენა + გადმოსაწევი",
    "tiltright" to "მარჯვენა + გადმოსაწევი"
)
val profileNames = listOf(
    "ალუმინი — ASAS", "ალუმინი — BENKAM",
    "მეტალოპლასტმასი — REHAU", "მეტალოპლასტმასი — ENDERPEN"
)
val colorNames = listOf(
    "თეთრი", "შავი მატი", "შავი NOIR 2100", "ანტრაციტი RAL 7016",
    "ნაცრისფერი RAL 7004", "ყავისფერი RAL 8017", "ვერცხლისფერი",
    "ოქროსფერი", "ხის ფაქტურა"
)
val glassNames = listOf("ერთმაგი", "ორმაგი 24მმ", "ორმაგი 32მმ", "სამმაგი 52მმ")

fun splitCell(s: FrameConstruction, id: String, axis: String): FrameConstruction? {
    val idx = s.cells.indexOfFirst { it.id == id }
    if (idx < 0 || s.cells.size >= 80) return null
    val old = s.cells[idx]
    if (axis == "vertical" && old.w * s.width < 160 ||
        axis == "horizontal" && old.h * s.height < 160) return null
    val one: FrameCell
    val two: FrameCell
    if (axis == "vertical") {
        one = old.copy(id = newId(), w = old.w / 2, opening = "fixed", direction = "in")
        two = old.copy(id = newId(), x = old.x + old.w / 2, w = old.w / 2, opening = "fixed", direction = "in")
    } else {
        one = old.copy(id = newId(), h = old.h / 2, opening = "fixed", direction = "in")
        two = old.copy(id = newId(), y = old.y + old.h / 2, h = old.h / 2, opening = "fixed", direction = "in")
    }
    return s.copy(cells = s.cells.toMutableList().apply {
        removeAt(idx); add(idx, two); add(idx, one)
    })
}
fun mergeCell(s: FrameConstruction, id: String): FrameConstruction? {
    val c = s.cells.find { it.id == id } ?: return null
    val e = 1e-7
    val other = s.cells.firstOrNull { p ->
        p.id != c.id && (
            abs(p.y - c.y) < e && abs(p.h - c.h) < e &&
                (abs(p.x + p.w - c.x) < e || abs(c.x + c.w - p.x) < e) ||
            abs(p.x - c.x) < e && abs(p.w - c.w) < e &&
                (abs(p.y + p.h - c.y) < e || abs(c.y + c.h - p.y) < e)
        )
    } ?: return null
    val x = min(c.x, other.x)
    val y = min(c.y, other.y)
    val r = max(c.x + c.w, other.x + other.w)
    val b = max(c.y + c.h, other.y + other.h)
    return s.copy(cells = s.cells.filter { it.id != id && it.id != other.id } +
        FrameCell(x = x, y = y, w = r - x, h = b - y))
}

data class DimensionHit(
    val kind: String,
    val start: Double = 0.0,
    val end: Double = 1.0,
    val side: String = "left",
    val old: Int = 0
)
fun changeDimension(s: FrameConstruction, hit: DimensionHit, requested: Int): FrameConstruction? {
    if (requested !in 100..15000) return null
    if (hit.kind == "width" || hit.kind == "segment" &&
        hit.start < 0.00001 && hit.end > 0.99999) return s.copy(width = requested)
    if (hit.kind == "height" || hit.kind == "vsegment" &&
        hit.start < 0.00001 && hit.end > 0.99999) return s.copy(height = requested)
    val eps = 0.00001
    if (hit.kind == "vsegment") {
        val H = s.height.toDouble()
        val boundary = if (hit.end > 0.99999) hit.start else hit.end
        val delta = requested - hit.old
        val next = if (hit.end > 0.99999) boundary - delta / H else boundary + delta / H
        if (next !in 0.07..0.93) return null
        val group = s.cells.filter {
            if (hit.side == "left") abs(it.x) < eps else abs(it.x + it.w - 1) < eps
        }
        val upper = group.filter { abs(it.y + it.h - boundary) < eps }
        val lower = group.filter { abs(it.y - boundary) < eps }
        if (upper.isEmpty() || lower.isEmpty()) return null
        if (upper.any { it.h + (next - boundary) < 100 / H } ||
            lower.any { it.h - (next - boundary) < 100 / H }) return null
        val changed = s.cells.map {
            when (it.id) {
                in upper.map { c -> c.id } -> it.copy(h = it.h + next - boundary)
                in lower.map { c -> c.id } -> it.copy(y = it.y + next - boundary, h = it.h - next + boundary)
                else -> it
            }
        }
        return s.copy(cells = changed)
    }
    if (hit.kind != "segment") return null
    val W = s.width.toDouble()
    val delta = requested - hit.old
    val boundary = hit.end * W + delta
    if (boundary <= hit.start * W + 100 ||
        hit.end < 0.99999 && boundary >= W - 100) return null
    if (hit.end > 0.99999) {
        val nextW = s.width + delta
        if (nextW !in 200..15000) return null
        val changed = s.cells.map { c ->
            val left = c.x * W
            var right = (c.x + c.w) * W
            if (abs(right - W) < .01) right = nextW.toDouble()
            c.copy(x = left / nextW, w = (right - left) / nextW)
        }
        return s.copy(width = nextW, cells = changed)
    }
    val left = hit.start * W
    val right = hit.end * W
    val ratio = (boundary - left) / (right - left)
    val otherRatio = (W - boundary) / (W - right)
    val changed = mutableListOf<FrameCell>()
    for (c in s.cells) {
        var x = c.x * W
        var r = (c.x + c.w) * W
        if (x >= right - .01) {
            x = boundary + (x - right) * otherRatio
            r = boundary + (r - right) * otherRatio
        } else if (x >= left - .01 && r <= right + .01) {
            x = left + (x - left) * ratio
            r = left + (r - left) * ratio
        } else if (x < right - .01 && r > right + .01) {
            return null
        }
        changed += c.copy(x = x / W, w = (r - x) / W)
    }
    return s.copy(cells = changed)
}

object OrderStore {
    private const val KEY = "framestudio_orders_native_v1"
    private const val SEQ = "framestudio_order_sequence_native_v1"
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("framestudio", Context.MODE_PRIVATE)
    fun read(ctx: Context): List<FrameOrder> = try {
        val array = JSONArray(prefs(ctx).getString(KEY, "[]"))
        (0 until array.length()).map { fromJson(array.getJSONObject(it)) }
    } catch (_: Exception) { emptyList() }
    fun write(ctx: Context, orders: List<FrameOrder>) {
        val a = JSONArray()
        orders.forEach { a.put(toJson(it)) }
        prefs(ctx).edit().putString(KEY, a.toString()).apply()
    }
    fun number(ctx: Context, orders: List<FrameOrder>): String {
        val period = SimpleDateFormat("MMyy", Locale.US).format(Date())
        var n = prefs(ctx).getInt(SEQ + period, 2000)
        for (o in orders) if (o.number.startsWith(period + "-")) {
            n = max(n, o.number.substringAfter("-").toIntOrNull() ?: 2000)
        }
        n++
        prefs(ctx).edit().putInt(SEQ + period, n).apply()
        return period + "-" + n
    }
    private fun toJson(c: FrameCell) = JSONObject().apply {
        put("id", c.id); put("x", c.x); put("y", c.y); put("w", c.w); put("h", c.h)
        put("opening", c.opening); put("direction", c.direction)
    }
    private fun toJson(c: FrameConstruction) = JSONObject().apply {
        put("id", c.id); put("kind", c.kind); put("profile", c.profile)
        put("color", c.color); put("glass", c.glass); put("notes", c.notes)
        put("width", c.width); put("height", c.height)
        put("cells", JSONArray().apply { c.cells.forEach { put(toJson(it)) } })
    }
    private fun toJson(o: FrameOrder) = JSONObject().apply {
        put("id", o.id); put("number", o.number); put("date", o.date)
        put("customer", o.customer); put("phone", o.phone); put("quantity", o.quantity)
        put("address", o.address)
        put("constructions", JSONArray().apply { o.constructions.forEach { put(toJson(it)) } })
    }
    private fun fromCell(j: JSONObject): FrameCell = FrameCell(
        id = j.optString("id", newId()), x = j.optDouble("x", 0.0),
        y = j.optDouble("y", 0.0), w = j.optDouble("w", 1.0), h = j.optDouble("h", 1.0),
        opening = j.optString("opening", "fixed"), direction = j.optString("direction", "in")
    )
    private fun fromConstruction(j: JSONObject): FrameConstruction {
        val arr = j.optJSONArray("cells") ?: JSONArray()
        val cells = (0 until arr.length()).mapNotNull {
            try { fromCell(arr.getJSONObject(it)) } catch (_: Exception) { null }
        }.filter { it.w > 0 && it.h > 0 && it.x >= 0 && it.y >= 0 &&
            it.x + it.w <= 1.00001 && it.y + it.h <= 1.00001 }
        return FrameConstruction(
            id = j.optString("id", newId()), kind = j.optString("kind", "ფანჯარა"),
            profile = j.optString("profile", ""), color = j.optString("color", ""),
            glass = j.optString("glass", ""), notes = j.optString("notes", ""),
            width = j.optInt("width", 1800), height = j.optInt("height", 1500),
            cells = cells.ifEmpty { listOf(FrameCell()) }
        )
    }
    private fun fromJson(j: JSONObject): FrameOrder {
        val arr = j.optJSONArray("constructions") ?: JSONArray()
        val constructions = (0 until arr.length()).mapNotNull {
            try { fromConstruction(arr.getJSONObject(it)) } catch (_: Exception) { null }
        }
        return FrameOrder(
            id = j.optString("id", newId()), number = j.optString("number", ""),
            date = j.optString("date", today()), customer = j.optString("customer", ""),
            phone = j.optString("phone", ""), quantity = j.optString("quantity", "1"),
            address = j.optString("address", ""),
            constructions = constructions.ifEmpty { listOf(FrameConstruction()) }
        )
    }
}
