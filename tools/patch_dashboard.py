from pathlib import Path
import re

main = Path('app/src/main/java/com/moneyfamily/app/MainActivity.kt')
s = main.read_text()

start = s.find('@Composable private fun Dashboard(')
end = s.find('private val PositiveColor=', start)
if start < 0 or end < 0:
    raise SystemExit('Dashboard boundaries not found')

dashboard = r'''@Composable private fun Dashboard(data:List<UiMovement>,month:Calendar,prev:()->Unit,next:()->Unit,add:()->Unit){
 var annualPage by remember{mutableStateOf(false)}
 val cur=data.filter{same(it.date,month)}
 val income=cur.filter{it.amount>0}.sumOf{it.amount}
 val expense=cur.filter{it.amount<0}.sumOf{it.amount}
 val balance=income+expense
 val catTotals=cur.groupBy{it.category.ifBlank{"Non classificata"}}.mapValues{(_,v)->v.sumOf{it.amount}}.filterValues{it!=0.0}.toList().sortedBy{it.second}
 val memberTotals=cur.groupBy{it.member.ifBlank{"Non assegnato"}}.mapValues{(_,v)->v.sumOf{it.amount}}.filterValues{it!=0.0}.toList().sortedByDescending{it.second}
 val typeTotals=cur.groupBy{it.typeName.takeIf{name->name.isNotBlank()&&!name.equals("EXPENSE",true)&&!name.equals("INCOME",true)}?:"Da classificare"}.mapValues{(_,v)->v.sumOf{it.amount}}.filterValues{it!=0.0}.toList().sortedBy{it.second}
 val goPrev={if(annualPage){annualPage=false}else{prev()}}
 val goNext={if(annualPage){annualPage=false;next()}else if(month.get(Calendar.MONTH)==Calendar.DECEMBER){annualPage=true}else{next()}}
 if(annualPage){AnnualSummaryPage(data,month,goPrev,goNext,add)}else{LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{MonthBar(mf.format(month.time),goPrev,goNext)}
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){MetricCard("Entrate",income,Modifier.weight(1f));MetricCard("Uscite",expense,Modifier.weight(1f));MetricCard("Saldo",balance,Modifier.weight(1f))}}
  item{PieChartCard("Composizione mensile",listOf("Entrate" to income,"Uscite" to expense))}
  item{BarChartCard("Totali per categoria",catTotals)}
  item{PieChartCard("Composizione per categoria",catTotals)}
  item{BarChartCard("Totali per componente",memberTotals)}
  item{BarChartCard("Totali per tipologia",typeTotals)}
  item{PieChartCard("Composizione per tipologia",typeTotals)}
  item{Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text("Riepilogo",style=MaterialTheme.typography.titleLarge);Text("Operazioni: ${cur.size}");Text("Spese: ${money.format(expense)}",color=NegativeColor);Text("Ricavi: ${money.format(income)}",color=PositiveColor);Text("Saldo: ${money.format(balance)}",color=if(balance<0)NegativeColor else PositiveColor)}}}
  item{if(month.get(Calendar.MONTH)==Calendar.DECEMBER){OutlinedButton(onClick={annualPage=true},modifier=Modifier.fillMaxWidth()){Text("Riepilogo annuale ${month.get(Calendar.YEAR)}")}}}
  item{Button(onClick=add,modifier=Modifier.fillMaxWidth()){Text("+ Inserisci operazione")}}
 }}
}

@Composable private fun AnnualSummaryPage(data:List<UiMovement>,month:Calendar,prev:()->Unit,next:()->Unit,add:()->Unit){
 val year=month.get(Calendar.YEAR)
 val yearly=data.filter{parse(it.date)?.get(Calendar.YEAR)==year}
 val income=yearly.filter{it.amount>0}.sumOf{it.amount}
 val expense=yearly.filter{it.amount<0}.sumOf{it.amount}
 val balance=income+expense
 val catTotals=yearly.groupBy{it.category.ifBlank{"Non classificata"}}.mapValues{(_,v)->v.sumOf{it.amount}}.filterValues{it!=0.0}.toList().sortedBy{it.second}
 val memberTotals=yearly.groupBy{it.member.ifBlank{"Non assegnato"}}.mapValues{(_,v)->v.sumOf{it.amount}}.filterValues{it!=0.0}.toList().sortedByDescending{it.second}
 val typeTotals=yearly.groupBy{it.typeName.takeIf{name->name.isNotBlank()&&!name.equals("EXPENSE",true)&&!name.equals("INCOME",true)}?:"Da classificare"}.mapValues{(_,v)->v.sumOf{it.amount}}.filterValues{it!=0.0}.toList().sortedBy{it.second}
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{MonthBar("Riepilogo $year",prev,next)}
  item{Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Riepilogo esercizio $year",style=MaterialTheme.typography.titleLarge);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){MetricCard("Entrate",income,Modifier.weight(1f));MetricCard("Uscite",expense,Modifier.weight(1f));MetricCard("Saldo",balance,Modifier.weight(1f))}}}}
  item{AnnualSummaryCard(data,month)}
  item{PieChartCard("Composizione esercizio $year",listOf("Entrate" to income,"Uscite" to expense))}
  item{BarChartCard("Totali per categoria — $year",catTotals)}
  item{PieChartCard("Composizione per categoria — $year",catTotals)}
  item{BarChartCard("Totali per componente — $year",memberTotals)}
  item{BarChartCard("Totali per tipologia — $year",typeTotals)}
  item{PieChartCard("Composizione per tipologia — $year",typeTotals)}
  item{Button(onClick=add,modifier=Modifier.fillMaxWidth()){Text("+ Inserisci operazione")}}
 }
}

'''
s = s[:start] + dashboard + s[end:]

# Replace the simple income/expense donut with a real segmented pie/donut chart.
pie_start = s.find('@Composable private fun PieChartCard(')
pie_end = s.find('@Composable private fun LegendRow', pie_start)
if pie_start < 0 or pie_end < 0:
    raise SystemExit('PieChartCard boundaries not found')

pie = r'''@Composable private fun PieChartCard(title:String,values:List<Pair<String,Double>>){
 val palette=listOf(
  androidx.compose.ui.graphics.Color(0xFF4F46E5),
  androidx.compose.ui.graphics.Color(0xFF16A34A),
  androidx.compose.ui.graphics.Color(0xFFEA580C),
  androidx.compose.ui.graphics.Color(0xFF0891B2),
  androidx.compose.ui.graphics.Color(0xFFDB2777),
  androidx.compose.ui.graphics.Color(0xFF7C3AED),
  androidx.compose.ui.graphics.Color(0xFFCA8A04),
  androidx.compose.ui.graphics.Color(0xFF0F766E),
  androidx.compose.ui.graphics.Color(0xFFDC2626),
  androidx.compose.ui.graphics.Color(0xFF475569)
 )
 val nonZero=values.filter{it.second!=0.0}
 val total=nonZero.sumOf{abs(it.second)}
 Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){
  Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   Text(title,style=MaterialTheme.typography.titleLarge)
   if(total==0.0){Text("Nessun dato")}
   else{
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
     Box(Modifier.size(158.dp),contentAlignment=Alignment.Center){
      Canvas(Modifier.fillMaxSize().padding(7.dp)){
       var start=0f
       nonZero.forEachIndexed{index,entry->
        val sweep=(abs(entry.second)/total*360.0).toFloat()
        drawArc(color=palette[index%palette.size],startAngle=start,sweepAngle=sweep,useCenter=false,style=androidx.compose.ui.graphics.drawscope.Stroke(width=42f,cap=androidx.compose.ui.graphics.StrokeCap.Butt))
        start+=sweep
       }
      }
      Column(horizontalAlignment=Alignment.CenterHorizontally){
       Text(money.format(nonZero.sumOf{it.second}),style=MaterialTheme.typography.titleMedium)
       Text("totale",style=MaterialTheme.typography.labelSmall)
      }
     }
     Spacer(Modifier.width(16.dp))
     Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
      nonZero.take(10).forEachIndexed{index,entry->
       val color=palette[index%palette.size]
       Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
        Box(Modifier.size(11.dp).clip(RoundedCornerShape(50)).background(color))
        Column(Modifier.weight(1f)){
         Text(entry.first,style=MaterialTheme.typography.labelLarge,maxLines=1)
         Text(money.format(entry.second),style=MaterialTheme.typography.bodyMedium,color=if(entry.second<0)NegativeColor else PositiveColor)
        }
       }
      }
     }
    }
   }
  }
 }
}

'''
s = s[:pie_start] + pie + s[pie_end:]
main.write_text(s)
