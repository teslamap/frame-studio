package ge.garantigroup.framestudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContent { MaterialTheme(colorScheme=darkColorScheme()) { Studio() } }
 }
}

@Composable
fun Studio() {
 var step by remember { mutableIntStateOf(0) }
 var customer by remember { mutableStateOf("") }
 var phone by remember { mutableStateOf("") }
 var address by remember { mutableStateOf("") }
 var width by remember { mutableStateOf("1200") }
 var height by remember { mutableStateOf("1500") }
 var vertical by remember { mutableStateOf(false) }
 var horizontal by remember { mutableStateOf(false) }
 var vRatio by remember { mutableFloatStateOf(.5f) }
 var hRatio by remember { mutableFloatStateOf(.5f) }
 var edit by remember { mutableStateOf("") }
 var value by remember { mutableStateOf("") }
 val navy=Color(0xFF0B1020)
 Column(Modifier.fillMaxSize().background(navy).padding(16.dp)) {
  Text("FRAME STUDIO",style=MaterialTheme.typography.headlineMedium,color=Color.White)
  Text("GARANTI GROUP · Native Android",color=Color.LightGray)
  Spacer(Modifier.height(12.dp))
  Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
   listOf("შეკვეთა","ნახაზი","დასრულება").forEachIndexed { i,title ->
    Button(onClick={step=i},modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=4.dp)) { Text(title,maxLines=1) }
   }
  }
  Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
   when(step) {
    0 -> {
     OutlinedTextField(customer,{customer=it},label={Text("დამკვეთი")},modifier=Modifier.fillMaxWidth())
     OutlinedTextField(phone,{phone=it},label={Text("ტელეფონი")},modifier=Modifier.fillMaxWidth())
     OutlinedTextField(address,{address=it},label={Text("მისამართი / ობიექტი")},modifier=Modifier.fillMaxWidth())
     OutlinedTextField(width,{width=it.filter(Char::isDigit)},label={Text("სიგანე (მმ)")},modifier=Modifier.fillMaxWidth())
     OutlinedTextField(height,{height=it.filter(Char::isDigit)},label={Text("სიმაღლე (მმ)")},modifier=Modifier.fillMaxWidth())
    }
    1 -> {
     Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
      Button(onClick={vertical=true},modifier=Modifier.weight(1f)) {Text("ვერტ. იმპოსტი")}
      Button(onClick={horizontal=true},modifier=Modifier.weight(1f)) {Text("ჰორ. იმპოსტი")}
     }
     Text("ორი თითით ზუმი და გადაადგილება",color=Color.LightGray)
     var scale by remember { mutableFloatStateOf(1f) }
     var pan by remember { mutableStateOf(Offset.Zero) }
     val w=width.toIntOrNull()?:1200
     val h=height.toIntOrNull()?:1500
     Canvas(Modifier.fillMaxWidth().height(370.dp).background(Color(0xFFEEF3FA))
      .pointerInput(Unit){detectTransformGestures { _,drag,zoom,_ ->
       scale=(scale*zoom).coerceIn(1f,4f)
       pan+=drag
      }}) {
       val fw=size.width*.72f*scale
       val fh=size.height*.65f*scale
       val x=(size.width-fw)/2+pan.x
       val y=(size.height-fh)/2+pan.y
       drawRect(Color(0xFFD8EDFA),Offset(x,y),androidx.compose.ui.geometry.Size(fw,fh))
       drawRect(Color(0xFF263E5A),Offset(x,y),androidx.compose.ui.geometry.Size(fw,fh),style=Stroke(8f))
       if(vertical)drawLine(Color(0xFF263E5A),Offset(x+fw*vRatio,y),Offset(x+fw*vRatio,y+fh),8f)
       if(horizontal)drawLine(Color(0xFF263E5A),Offset(x,y+fh*hRatio),Offset(x+fw,y+fh*hRatio),8f)
      }
     Button(onClick={edit="width";value=width}) {Text("სიგანე: $w მმ — შეცვლა")}
     Button(onClick={edit="height";value=height}) {Text("სიმაღლე: $h მმ — შეცვლა")}
     if(vertical)Button(onClick={edit="vertical";value=(w*vRatio).roundToInt().toString()}) {Text("მარცხენა სექცია: ${(w*vRatio).roundToInt()} მმ — შეცვლა")}
     if(horizontal)Button(onClick={edit="horizontal";value=(h*hRatio).roundToInt().toString()}) {Text("ზედა სექცია: ${(h*hRatio).roundToInt()} მმ — შეცვლა")}
     OutlinedButton(onClick={vertical=false;horizontal=false}) {Text("იმპოსტების გასუფთავება")}
    }
    else -> {
     Text("შეკვეთის გადახედვა",style=MaterialTheme.typography.titleLarge)
     Text("დამკვეთი: $customer\nტელეფონი: $phone\nმისამართი: $address\nკონსტრუქცია: $width × $height მმ")
     Text("PDF და WhatsApp შემდეგ ვერსიაში დაემატება.",color=Color(0xFFFFC777))
    }
   }
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
   OutlinedButton(onClick={if(step>0)step--},enabled=step>0) {Text("უკან")}
   Button(onClick={if(step<2)step++},enabled=step<2) {Text("შემდეგი")}
  }
 }
 if(edit.isNotEmpty())AlertDialog(onDismissRequest={edit=""},title={Text("ზომის შეცვლა (მმ)")},
  text={OutlinedTextField(value,{value=it.filter(Char::isDigit)})},
  confirmButton={TextButton(onClick={
   val n=value.toIntOrNull()
   if(n!=null&&n>=100) {
    when(edit) {
     "width"->width=n.toString()
     "height"->height=n.toString()
     "vertical"->vRatio=(n.toFloat()/(width.toIntOrNull()?:1200)).coerceIn(.08f,.92f)
     "horizontal"->hRatio=(n.toFloat()/(height.toIntOrNull()?:1500)).coerceIn(.08f,.92f)
    }
   }
   edit=""
  }) {Text("შენახვა")}},
  dismissButton={TextButton(onClick={edit=""}){Text("გაუქმება")}})
}
