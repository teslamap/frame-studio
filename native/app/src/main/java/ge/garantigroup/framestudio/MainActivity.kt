package ge.garantigroup.framestudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.min

private val Background = Color(0xFF0B1020)
private val Panel = Color(0xFF141D31)
private val Input = Color(0xFF0E172A)
private val PanelBorder = Color(0xFF29354E)
private val ButtonBase = Color(0xFF202C46)
private val ButtonBorder = Color(0xFF36435E)
private val Blue = Color(0xFF397BF4)
private val Foreground = Color(0xFFE9EFFB)
private val Secondary = Color(0xFF9BAAC3)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(11,16,32)
        window.navigationBarColor = android.graphics.Color.rgb(11,16,32)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary=Blue, background=Background, surface=Panel,
                onSurface=Foreground, onBackground=Foreground
            )) {
                FrameStudioScreen()
            }
        }
    }
}

@Composable
private fun FrameStudioScreen() {
    val ctx=LocalContext.current
    var order by remember { mutableStateOf(FrameOrder()) }
    var saved by remember { mutableStateOf(OrderStore.read(ctx)) }
    var step by remember { mutableIntStateOf(0) }
    var active by remember { mutableIntStateOf(0) }
    var tool by remember { mutableStateOf("select") }
    var selected by remember { mutableStateOf<String?>(order.constructions[0].cells[0].id) }
    var undo by remember { mutableStateOf(emptyList<FrameConstruction>()) }
    var status by remember { mutableStateOf("მზადაა") }
    var addDialog by remember { mutableStateOf(false) }
    var openingDialog by remember { mutableStateOf(false) }
    var editHit by remember { mutableStateOf<DimensionHit?>(null) }
    var editText by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmNew by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<String?>(null) }

    val frame=order.constructions[active.coerceIn(0, order.constructions.lastIndex)]
    fun updateFrame(updated: FrameConstruction, pushUndo: Boolean = false) {
        if (pushUndo) undo=(undo+frame).takeLast(40)
        order=order.copy(constructions=order.constructions.toMutableList().apply { this[active]=updated })
    }
    fun changeFrame(block: (FrameConstruction)->FrameConstruction, pushUndo: Boolean=false) {
        updateFrame(block(frame),pushUndo)
    }
    fun saveOrder() {
        val current=if(order.number.isBlank())
            order.copy(number=OrderStore.number(ctx,saved),date=today()) else order
        val newSaved=listOf(current)+saved.filter { it.id!=current.id }
        OrderStore.write(ctx,newSaved)
        saved=newSaved
        order=current
        status="შენახულია ✓ · "+current.number
    }
    fun resetOrder() {
        order=FrameOrder()
        active=0
        selected=order.constructions[0].cells[0].id
        tool="select"
        undo=emptyList()
        status="ახალი შეკვეთა"
        step=0
    }
    val scroll=rememberScrollState()
    LaunchedEffect(step) { scroll.scrollTo(0) }

    Column(Modifier.fillMaxSize().background(Background).padding(horizontal=10.dp, vertical=12.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("FRAME",fontSize=22.sp,fontWeight=FontWeight.ExtraBold,color=Foreground)
            Text("STUDIO",fontSize=22.sp,fontWeight=FontWeight.ExtraBold,color=Color(0xFF69A7FF))
            Spacer(Modifier.width(7.dp))
            Text("OFFLINE",fontSize=10.sp,color=Color(0xFF69A7FF))
        }
        Text("კარ-ფანჯრის შეკვეთების ვიზუალური კონსტრუქტორი · ინტერნეტის გარეშე",
            color=Secondary,fontSize=11.sp,lineHeight=16.sp)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("შეკვეთის მონაცემები","ნახაზი","დასრულება").forEachIndexed { index,title ->
                val done=index<step
                val activeStep=index==step
                Column(Modifier.weight(1f).heightIn(min=62.dp)
                    .background(if(activeStep) Color(0xFF2058A5) else Color(0xFF14223B),RoundedCornerShape(13.dp))
                    .border(1.dp,if(activeStep) Color(0xFF5C9BFF) else if(done) Color(0xFF3C8F9E) else Color(0xFF344760),
                        RoundedCornerShape(13.dp))
                    .clickable { step=index }.padding(vertical=10.dp,horizontal=3.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                    Text("ნაბიჯი "+(index+1),fontSize=10.sp,color=Secondary)
                    Spacer(Modifier.height(3.dp))
                    Text(title,fontSize=11.sp,fontWeight=FontWeight.SemiBold,maxLines=1,
                        overflow=TextOverflow.Ellipsis,color=if(activeStep) Color.White else Foreground)
                }
            }
        }
        Spacer(Modifier.height(13.dp))
        Column(Modifier.weight(1f).verticalScroll(scroll)) {
            when(step) {
                0 -> {
                    StudioPanel("შეკვეთის დეტალები") {
                        StudioField("დამკვეთი",order.customer,{order=order.copy(customer=it)},
                            "სახელი / კომპანია")
                        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f)) {
                                StudioField("ტელეფონი",order.phone,{order=order.copy(phone=it)},
                                    keyboard=KeyboardType.Phone)
                            }
                            Box(Modifier.weight(1f)) {
                                StudioField("რაოდენობა",order.quantity,
                                    {order=order.copy(quantity=it.filter(Char::isDigit))},
                                    keyboard=KeyboardType.Number)
                            }
                        }
                        StudioField("მისამართი / ობიექტი",order.address,{order=order.copy(address=it)})
                    }
                    Spacer(Modifier.height(14.dp))
                    StudioPanel("კონსტრუქცია") {
                        StudioSelect("კონსტრუქციის ტიპი",frame.kind,
                            listOf("ფანჯარა","კარი","ვიტრაჟი","სხვა"),
                            onChange={ v -> changeFrame({ it.copy(kind=v) }) })
                        StudioSelect("პროფილი / სისტემა",frame.profile,profileNames,
                            {v->changeFrame({it.copy(profile=v)})},"აირჩიე პროფილი / სისტემა")
                        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f)) {
                                StudioSelect("ფერი",frame.color,colorNames,
                                    {v->changeFrame({it.copy(color=v)})},"აირჩიე ფერი")
                            }
                            Box(Modifier.weight(1f)) {
                                StudioSelect("მინა / მინაპაკეტი",frame.glass,glassNames,
                                    {v->changeFrame({it.copy(glass=v)})},"აირჩიე მინაპაკეტი")
                            }
                        }
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Box(Modifier.size(23.dp).background(
                                when(frame.color) {
                                    "თეთრი"->Color(0xFFF7F7F5)
                                    "შავი მატი"->Color(0xFF202124)
                                    "შავი NOIR 2100"->Color(0xFF17191C)
                                    "ანტრაციტი RAL 7016"->Color(0xFF383E42)
                                    "ნაცრისფერი RAL 7004"->Color(0xFF9B9D9A)
                                    "ყავისფერი RAL 8017"->Color(0xFF44312B)
                                    "ვერცხლისფერი"->Color(0xFFC0C4C7)
                                    "ოქროსფერი"->Color(0xFFC5A16C)
                                    "ხის ფაქტურა"->Color(0xFF94623D)
                                    else->Color(0xFF263E5A)
                                },RoundedCornerShape(6.dp))
                                .border(1.dp,Secondary,RoundedCornerShape(6.dp)))
                            Spacer(Modifier.width(8.dp))
                            Text(frame.color.ifBlank { "ნაგულისხმევი" },fontSize=12.sp,color=Secondary)
                        }
                        Spacer(Modifier.height(10.dp))
                        StudioField("შენიშვნები",frame.notes,
                            {v->changeFrame({it.copy(notes=v)})},
                            "დამატებითი მოთხოვნები, მონტაჟი, მიმართულებები...",singleLine=false)
                    }
                }
                1 -> {
                    Column(Modifier.background(Color(0xFF101C31),RoundedCornerShape(14.dp))
                        .border(1.dp,Color(0xFF344760),RoundedCornerShape(14.dp)).padding(13.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Text("კონსტრუქციები",fontWeight=FontWeight.Bold,color=Foreground)
                            Spacer(Modifier.weight(1f))
                            StudioButton("＋ ახალი ნახაზი",primary=true) { addDialog=true }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                            order.constructions.forEachIndexed { index,c ->
                                StudioButton((index+1).toString()+". "+c.kind,
                                    primary=index==active) {
                                    active=index;selected=null;undo=emptyList();tool="select"
                                }
                                if(order.constructions.size>1) {
                                    StudioButton("×",danger=true) {
                                        val all=order.constructions.toMutableList()
                                        all.removeAt(index)
                                        order=order.copy(constructions=all)
                                        active=min(active,all.lastIndex)
                                        selected=null;undo=emptyList()
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(9.dp))
                        Text("ნახაზის დასრულების შემდეგ დააჭირე „＋ ახალი ნახაზი“ — გაიხსნება ცარიელი კონსტრუქცია. წინა ნახაზი შენარჩუნდება.",
                            fontSize=12.sp,color=Secondary,lineHeight=18.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    StudioPanel("ნახაზის კონსტრუქტორი") {
                        Text(if(selected==null) "უჯრა არჩეული არაა" else
                            "უჯრა "+(frame.cells.indexOfFirst{it.id==selected}+1),
                            color=Color(0xFFBAD4FF),fontSize=12.sp,
                            modifier=Modifier.background(Color(0xFF22385A),RoundedCornerShape(8.dp))
                                .padding(horizontal=8.dp,vertical=5.dp))
                        Spacer(Modifier.height(11.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            StudioButton("↖ არჩევა",primary=tool=="select") { tool="select" }
                            StudioButton("┃ ვერტ. იმპოსტი",primary=tool=="vertical") { tool="vertical" }
                            StudioButton("━ ჰორ. იმპოსტი",primary=tool=="horizontal") { tool="horizontal" }
                            StudioButton("⊞ გაყოფის გაუქმება",enabled=selected!=null&&frame.cells.size>1) {
                                val result=mergeCell(frame,selected!!)
                                if(result==null) status="ამ უჯრის გაერთიანება შეუძლებელია"
                                else {
                                    updateFrame(result,true)
                                    selected=result.cells.lastOrNull()?.id
                                }
                            }
                            StudioButton("↶ უკან",enabled=undo.isNotEmpty()) {
                                if(undo.isNotEmpty()) {
                                    updateFrame(undo.last())
                                    undo=undo.dropLast(1)
                                    selected=null
                                }
                            }
                            StudioButton("✕ ნახაზის გასუფთავება",danger=true) { confirmClear=true }
                        }
                        Spacer(Modifier.height(12.dp))
                        FrameDrawing(frame,selected,tool,
                            onCellTap={ id ->
                                if(tool=="select") selected=id
                                else {
                                    val result=splitCell(frame,id,tool)
                                    if(result==null) status="უჯრა ძალიან პატარაა ან ლიმიტი ამოწურულია"
                                    else {
                                        updateFrame(result,true)
                                        selected=result.cells.firstOrNull { it.id !in frame.cells.map { c->c.id } }?.id
                                        tool="select"
                                    }
                                }
                            },
                            onCellLongPress={ id-> selected=id;openingDialog=true },
                            onDimension={ hit->editHit=hit;editText=hit.old.toString() },
                            onImpostDragStart={undo=(undo+frame).takeLast(40)},
                            onImpostDrag={ d,pos ->
                                val moved=moveImpost(frame,d,pos)
                                if(moved!=null) updateFrame(moved.construction)
                                moved?.position
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("აირჩიე ვერტიკალური ან ჰორიზონტალური იმპოსტი და დააჭირე უჯრას — იმპოსტი შუაში ჩაისმება. გაღების ტიპისთვის უჯრას დიდხანს დააჭირე. ლურჯ ზომებზე შეხებით შეცვალე ზომები პირდაპირ ნახაზზე.",
                            fontSize=12.sp,lineHeight=18.sp,color=Secondary,
                            modifier=Modifier.background(Color(0xFF101A2C),RoundedCornerShape(10.dp))
                                .padding(12.dp))
                        Spacer(Modifier.height(10.dp))
                        Row {
                            Text(frame.cells.size.toString()+" უჯრა · "+frame.width+" × "+
                                frame.height+" მმ",fontSize=12.sp,color=Color(0xFFA9C6FF))
                            Spacer(Modifier.weight(1f))
                            Text(status,fontSize=12.sp,color=Color(0xFFA9C6FF))
                        }
                    }
                }
                else -> {
                    StudioPanel("შეკვეთის გადამოწმება") {
                        Text("გადაამოწმე მონაცემები. შენახვა და PDF-ის გაზიარება ხელმისაწვდომია ამ ბოლო ნაბიჯზე.",
                            fontSize=12.sp,color=Secondary,lineHeight=18.sp)
                        Spacer(Modifier.height(14.dp))
                        listOf(
                            "შეკვეთის №" to order.number.ifBlank{"—"},
                            "თარიღი" to order.date,
                            "დამკვეთი" to order.customer,
                            "ტელეფონი" to order.phone,
                            "მისამართი" to order.address,
                            "რაოდენობა" to order.quantity,
                            "სიგანე" to frame.width.toString(),
                            "სიმაღლე" to frame.height.toString(),
                            "ტიპი" to frame.kind,
                            "პროფილი" to frame.profile,
                            "ფერი" to frame.color,
                            "მინაპაკეტი" to frame.glass,
                            "შენიშვნები" to frame.notes
                        ).forEach { (name,v) ->
                            Column(Modifier.fillMaxWidth()
                                .background(Color(0xFF1B2C47),RoundedCornerShape(9.dp))
                                .padding(12.dp)) {
                                Text(name,fontSize=11.sp,color=Color(0xFF9BB0CA))
                                Text(v.ifBlank{"—"},fontWeight=FontWeight.Bold,color=Foreground)
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        Text("კონსტრუქციების რაოდენობა: "+order.constructions.size,
                            fontWeight=FontWeight.Bold,color=Foreground)
                        Spacer(Modifier.height(7.dp))
                        order.constructions.forEachIndexed { i,c ->
                            Text((i+1).toString()+". "+c.kind+" — "+c.width+
                                " × "+c.height+" მმ",color=Foreground)
                        }
                        Spacer(Modifier.height(16.dp))
                        StudioButton("▣ შეკვეთის შენახვა",primary=true) { saveOrder() }
                        Spacer(Modifier.height(8.dp))
                        StudioButton("↗ PDF WhatsApp-ზე") {
                            runCatching { FramePdf.shareOnWhatsApp(ctx,order) }
                                .onFailure { status="PDF შეცდომა: "+it.localizedMessage }
                        }
                        Spacer(Modifier.height(8.dp))
                        StudioButton("＋ ახალი შეკვეთა") { confirmNew=true }
                    }
                    Spacer(Modifier.height(14.dp))
                    StudioPanel("შენახული შეკვეთები") {
                        if(saved.isEmpty()) {
                            Text("ჯერ არაფერი შენახულა.",color=Secondary,fontSize=12.sp)
                        } else saved.forEach { s ->
                            Row(Modifier.fillMaxWidth()
                                .background(Color(0xFF10192B),RoundedCornerShape(9.dp))
                                .padding(7.dp),verticalAlignment=Alignment.CenterVertically) {
                                Text((s.number.ifBlank{"№ —"})+" · "+s.customer.ifBlank{"უსახელო"}+
                                    "\n"+s.date,color=Foreground,fontSize=12.sp,
                                    modifier=Modifier.weight(1f).clickable {
                                        order=s;active=0;selected=null;undo=emptyList()
                                        tool="select";step=0;status="შეკვეთა გახსნილია"
                                    })
                                Spacer(Modifier.width(6.dp))
                                StudioButton("✕",danger=true) { deleteId=s.id }
                            }
                            Spacer(Modifier.height(7.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(15.dp))
        }
        Column(Modifier.fillMaxWidth()
            .background(Color(0xFF17253E),RoundedCornerShape(15.dp))
            .border(1.dp,Color(0xFF344760),RoundedCornerShape(15.dp))
            .padding(10.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                StudioButton("← უკან",enabled=step>0) { if(step>0)step-- }
                Spacer(Modifier.weight(1f))
                Text((step+1).toString()+" / 3",fontSize=13.sp,color=Color(0xFFB6C6DA))
                Spacer(Modifier.weight(1f))
                StudioButton(if(step==2) "▣ შენახვა" else "შემდეგი →",primary=true) {
                    if(step==2) saveOrder() else step++
                }
            }
            if(status.startsWith("PDF შეცდომა")) {
                Spacer(Modifier.height(7.dp))
                Text(status,color=Color(0xFFFFB3B3),fontSize=11.sp)
            }
        }
    }

    if(addDialog) {
        Dialog(onDismissRequest={addDialog=false}) {
            Column(Modifier.fillMaxWidth().background(Color.White,RoundedCornerShape(20.dp)).padding(20.dp)) {
                Text("რა კონსტრუქცია გინდა?",fontSize=19.sp,
                    fontWeight=FontWeight.Bold,color=Color(0xFF152845))
                Spacer(Modifier.height(16.dp))
                listOf("ფანჯარა","კარი","ვიტრაჟი","სხვა").chunked(2).forEach { pair ->
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        pair.forEach { kind ->
                            Column(Modifier.weight(1f).height(94.dp)
                                .background(Color(0xFFEFF5FF),RoundedCornerShape(13.dp))
                                .border(1.dp,Color(0xFFCBDaf2),RoundedCornerShape(13.dp))
                                .clickable {
                                    order=order.copy(constructions=order.constructions+FrameConstruction(kind=kind))
                                    active=order.constructions.lastIndex
                                    selected=order.constructions[active].cells[0].id
                                    tool="select";undo=emptyList();addDialog=false
                                }.padding(10.dp),
                                horizontalAlignment=Alignment.CenterHorizontally,
                                verticalArrangement=Arrangement.Center) {
                                Text(when(kind){"ფანჯარა"->"▣";"კარი"->"▯";"ვიტრაჟი"->"▥";else->"⊞"},
                                    fontSize=28.sp,color=Color(0xFF1E5FBE))
                                Text(kind,fontSize=14.sp,color=Color(0xFF243D63))
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                StudioButton("გაუქმება") { addDialog=false }
            }
        }
    }
    val currentlySelected=frame.cells.find{it.id==selected}
    if(openingDialog && currentlySelected!=null) {
        Dialog(onDismissRequest={openingDialog=false}) {
            Column(Modifier.fillMaxWidth().background(Color(0xFF14213A),RoundedCornerShape(18.dp))
                .border(1.dp,Color(0xFF42618B),RoundedCornerShape(18.dp))
                .padding(18.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("გაღების ტიპი",fontSize=18.sp,fontWeight=FontWeight.Bold,color=Foreground)
                    Spacer(Modifier.weight(1f))
                    StudioButton("✕") { openingDialog=false }
                }
                Spacer(Modifier.height(12.dp))
                openingNames.forEach { (key,label) ->
                    StudioButton(if(currentlySelected.opening==key) "✓ "+label else label,
                        primary=currentlySelected.opening==key) {
                        changeFrame({f->f.copy(cells=f.cells.map{c->
                            if(c.id==currentlySelected.id)c.copy(opening=key) else c
                        })},true)
                    }
                    Spacer(Modifier.height(7.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("გაღების მიმართულება",fontSize=16.sp,
                    fontWeight=FontWeight.Bold,color=Foreground)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        StudioButton("↙ შიგნით გაღება",
                            primary=currentlySelected.direction=="in") {
                            changeFrame({f->f.copy(cells=f.cells.map{c->
                                if(c.id==currentlySelected.id)c.copy(direction="in") else c
                            })},true)
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        StudioButton("↗ გარეთ გაღება",
                            primary=currentlySelected.direction=="out") {
                            changeFrame({f->f.copy(cells=f.cells.map{c->
                                if(c.id==currentlySelected.id)c.copy(direction="out") else c
                            })},true)
                        }
                    }
                }
            }
        }
    }
    if(editHit!=null) {
        AlertDialog(
            onDismissRequest={editHit=null},
            title={Text("ახალი ზომა (მმ)")},
            text={
                StudioField("შეიყვანე ზომა",editText,
                    {editText=it.filter(Char::isDigit)},keyboard=KeyboardType.Number)
            },
            confirmButton={
                TextButton(onClick={
                    val requested=editText.toIntOrNull()
                    val updated=if(requested==null) null else changeDimension(frame,editHit!!,requested)
                    if(updated==null) status="ამ სექციის ზომის შეცვლა შეუძლებელია"
                    else { updateFrame(updated,true);status="ზომა შეცვლილია ✓" }
                    editHit=null
                }) {Text("შენახვა")}
            },
            dismissButton={TextButton(onClick={editHit=null}){Text("გაუქმება")}}
        )
    }
    if(confirmClear) {
        ConfirmStudio("ნახაზის გასუფთავება","გავასუფთავოთ ნახაზი?",
            onYes={updateFrame(frame.copy(cells=listOf(FrameCell())),true);selected=null;confirmClear=false},
            onNo={confirmClear=false})
    }
    if(confirmNew) {
        ConfirmStudio("ახალი შეკვეთა",
            "მიმდინარე შეუნახავი ცვლილებები დაიკარგება.",
            onYes={resetOrder();confirmNew=false},onNo={confirmNew=false})
    }
    if(deleteId!=null) {
        ConfirmStudio("შეკვეთის წაშლა","წავშალოთ შენახული შეკვეთა?",
            onYes={
                saved=saved.filter { it.id!=deleteId }
                OrderStore.write(ctx,saved)
                deleteId=null
            },onNo={deleteId=null})
    }
}

@Composable
private fun ConfirmStudio(title:String,message:String,onYes:()->Unit,onNo:()->Unit) {
    AlertDialog(onDismissRequest=onNo,title={Text(title)},text={Text(message)},
        confirmButton={TextButton(onClick=onYes){Text("დიახ")}},
        dismissButton={TextButton(onClick=onNo){Text("გაუქმება")}})
}

@Composable
private fun StudioPanel(title:String,content:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(17.dp))
        .border(1.dp,PanelBorder,RoundedCornerShape(17.dp))
        .padding(14.dp)) {
        Text(title,fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=Foreground)
        Spacer(Modifier.height(15.dp))
        content()
    }
}
@Composable
private fun StudioField(label:String,value:String,onValue:(String)->Unit,
    placeholder:String="",keyboard:KeyboardType=KeyboardType.Text,singleLine:Boolean=true) {
    Column(Modifier.fillMaxWidth()) {
        Text(label,color=Color(0xFFA9B9D3),fontSize=12.sp)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value=value,onValueChange=onValue,
            singleLine=singleLine,
            keyboardOptions=KeyboardOptions(keyboardType=keyboard),
            cursorBrush=SolidColor(Color(0xFF609BFF)),
            textStyle=TextStyle(color=Color(0xFFF4F7FF),fontSize=14.sp,lineHeight=20.sp),
            modifier=Modifier.fillMaxWidth()
                .background(Input,RoundedCornerShape(9.dp))
                .border(1.dp,Color(0xFF34415A),RoundedCornerShape(9.dp))
                .defaultMinSize(minHeight=44.dp)
                .padding(11.dp),
            decorationBox={inner ->
                Box {
                    if(value.isEmpty()&&placeholder.isNotBlank())
                        Text(placeholder,color=Secondary,fontSize=13.sp)
                    inner()
                }
            }
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StudioSelect(label:String,value:String,items:List<String>,
    onChange:(String)->Unit,placeholder:String="აირჩიე") {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(label,fontSize=12.sp,color=Color(0xFFA9B9D3))
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().heightIn(min=44.dp)
                .background(Input,RoundedCornerShape(9.dp))
                .border(1.dp,Color(0xFF34415A),RoundedCornerShape(9.dp))
                .clickable { expanded=true }
                .padding(horizontal=10.dp,vertical=12.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Text(value.ifBlank{placeholder},modifier=Modifier.weight(1f),
                    fontSize=12.sp,maxLines=2,color=if(value.isBlank()) Secondary else Foreground)
                Text("⌄",color=Secondary,fontSize=17.sp)
            }
            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false},
                modifier=Modifier.background(Panel)) {
                DropdownMenuItem(text={Text("— აირჩიე —",color=Foreground)},
                    onClick={onChange("");expanded=false})
                items.forEach {
                    DropdownMenuItem(text={Text(it,color=Foreground)},
                        onClick={onChange(it);expanded=false})
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StudioButton(label:String,primary:Boolean=false,
    danger:Boolean=false,enabled:Boolean=true,onClick:()->Unit) {
    Box(Modifier.heightIn(min=42.dp)
        .background(if(primary) Color(0xFF397BF4) else ButtonBase,RoundedCornerShape(10.dp))
        .border(1.dp,if(primary) Blue else if(danger) Color(0xFF754552) else ButtonBorder,
            RoundedCornerShape(10.dp))
        .clickable(enabled=enabled,onClick=onClick)
        .padding(horizontal=12.dp,vertical=10.dp),
        contentAlignment=Alignment.Center) {
        Text(label,fontSize=12.sp,maxLines=2,
            color=when { !enabled->Color(0xFF7886A1);danger->Color(0xFFFFB3B3);else->Foreground },
            fontWeight=if(primary) FontWeight.SemiBold else FontWeight.Normal)
    }
}
