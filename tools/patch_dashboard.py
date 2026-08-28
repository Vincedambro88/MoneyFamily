from pathlib import Path
import re

p = Path('app/src/main/java/com/moneyfamily/app/MainActivity.kt')
s = p.read_text()

s = s.replace(
    'data class UiMovement(val id:Long,val type:MovementType,val amount:Double,val category:String,val description:String,val date:String,val member:String)',
    'data class UiMovement(val id:Long,val type:MovementType,val amount:Double,val category:String,val description:String,val date:String,val member:String,val typeName:String = "")'
)

start = s.index('@Composable private fun Dashboard(')
end = s.index('@Composable private fun Operations(', start)
new_dashboard = r'''@Composable private fun Dashboard(data:List<UiMovement>,month:Calendar,prev:()->Unit,next:()->Unit,add:()->Unit){
 val cur=data.filter{same(it.date,month)}
 val income=cur.filter{it.amount>0}.sumOf{it.amount}
 val expense=cur.filter{it.amount<0}.sumOf{it.amount}
 val balance=income+expense
 val catTotals=cur.groupBy{it.category.ifBlank{"Non classificata"}}.mapValues{(_,v)->v.sumOf{it.amount}}.filterValues{it!=0.0}.toList().sortedBy{it.second}
 val memberTotals=cur.groupBy{it.member.ifBlank{"Non assegnato"}}.mapValues{(_,v)->v.sumOf{it.amount}}.filterValues{it!=0.0}.toList().sortedByDescending{it.second}
 val typeTotals=cur.groupBy{it.typeName.ifBlank{"Non classificata"}}.mapValues{(_,v)->v.sumOf{it.amount}}.filterValues{it!=0.0}.toList().sortedBy{it.second}
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{MonthBar(mf.format(month.time),prev,next)}
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){MetricCard("Entrate",income,Modifier.weight(1f));MetricCard("Uscite",expense,Modifier.weight(1f));MetricCard("Saldo",balance,Modifier.weight(1f))}}
  item{PieChartCard("Composizione mensile",listOf("Entrate" to income,"Uscite" to expense))}
  item{BarChartCard("Totali per categoria",catTotals)}
  item{BarChartCard("Totali per componente",memberTotals)}
  item{BarChartCard("Totali per tipologia",typeTotals)}
  item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Text("Riepilogo",style=MaterialTheme.typography.titleLarge);Text("Operazioni: ${cur.size}");Text("Spese: ${money.format(expense)}",color=NegativeColor);Text("Ricavi: ${money.format(income)}",color=PositiveColor);Text("Saldo: ${money.format(balance)}",color=if(balance<0)NegativeColor else PositiveColor)}}}
  item{Button(onClick=add,modifier=Modifier.fillMaxWidth()){Text("+ Inserisci operazione")}}
 }
}

private val PositiveColor=androidx.compose.ui.graphics.Color(0xFF2E7D32)
private val NegativeColor=androidx.compose.ui.graphics.Color(0xFFC62828)

@Composable private fun MetricCard(title:String,value:Double,modifier:Modifier){Card(modifier){Column(Modifier.padding(10.dp)){Text(title,style=MaterialTheme.typography.labelLarge);Text(money.format(value),style=MaterialTheme.typography.titleMedium,color=if(value<0)NegativeColor else PositiveColor)}}}

@Composable private fun PieChartCard(title:String,values:List<Pair<String,Double>>){
 val positive=values.sumOf{if(it.second>0)it.second else 0.0}
 val negative=values.sumOf{if(it.second<0)kotlin.math.abs(it.second) else 0.0}
 val total=positive+negative
 Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Text(title,style=MaterialTheme.typography.titleLarge)
  if(total==0.0) Text("Nessun movimento nel mese") else {
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
    Canvas(Modifier.size(150.dp)){
     var start=0f
     if(positive>0){val sweep=(positive/total*360.0).toFloat();drawArc(PositiveColor,start,sweep,true);start+=sweep}
     if(negative>0){val sweep=(negative/total*360.0).toFloat();drawArc(NegativeColor,start,sweep,true)}
    }
    Spacer(Modifier.width(16.dp))
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
     if(positive>0)LegendRow("Entrate",positive,PositiveColor)
     if(negative>0)LegendRow("Uscite",-negative,NegativeColor)
    }
   }
  }
 }}
}

@Composable private fun LegendRow(name:String,value:Double,color:androidx.compose.ui.graphics.Color){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color));Text("$name  ${money.format(value)}")}}

@Composable private fun BarChartCard(title:String,values:List<Pair<String,Double>>){Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){Text(title,style=MaterialTheme.typography.titleLarge);if(values.isEmpty())Text("Nessun dato per il mese")else{val maxAbs=values.maxOf{abs(it.second)}.coerceAtLeast(1.0);values.take(12).forEach{(n,v)->BarChartRow(n,v,maxAbs)}}}}}

@Composable private fun BarChartRow(name:String,value:Double,maxAbs:Double){val fraction=(abs(value)/maxAbs).toFloat().coerceIn(0f,1f);val color=if(value<0)NegativeColor else PositiveColor;Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(name,Modifier.weight(1f));Text(money.format(value),color=color)};Box(Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(9.dp)).background(MaterialTheme.colorScheme.surfaceVariant)){if(fraction>0)Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(color))}}}

'''
s = s[:start] + new_dashboard + s[end:]

s = s.replace('(type.isBlank()||it.type.name==type)', '(type.isBlank()||it.typeName==type)')
s = s.replace('Text("${x.type} • ${x.category} • ${x.member} • ${x.date}")', 'Text("${x.typeName.ifBlank{"Non classificata"}} • ${x.category} • ${x.member} • ${x.date}")')
s = s.replace('save(UiMovement(old?.id?:System.currentTimeMillis(),if(a<0)MovementType.EXPENSE else MovementType.INCOME,a,category,desc,date,member))', 'save(UiMovement(old?.id?:System.currentTimeMillis(),if(a<0)MovementType.EXPENSE else MovementType.INCOME,a,category,desc,date,member,type))')
s = s.replace('private fun Movement.ui()=UiMovement(id,type,amount,category,description,date,member)', 'private fun Movement.ui()=UiMovement(id,type,amount,category,description,date,member,typeName)')
s = s.replace('private fun UiMovement.model()=Movement(id,type,amount,category,description,date,member,"")', 'private fun UiMovement.model()=Movement(id,type,amount,category,description,date,member,"",typeName)')
s = s.replace('import androidx.compose.foundation.layout.*', 'import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.Canvas')
s = s.replace('import androidx.compose.ui.unit.dp', 'import androidx.compose.ui.unit.dp\nimport kotlin.math.abs')

p.write_text(s)
print('patched MainActivity.kt')
