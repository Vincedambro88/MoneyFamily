from pathlib import Path

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
  item{BarChartCard("Totali per componente",memberTotals)}
  item{BarChartCard("Totali per tipologia",typeTotals)}
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
  item{BarChartCard("Totali per componente — $year",memberTotals)}
  item{BarChartCard("Totali per tipologia — $year",typeTotals)}
  item{Button(onClick=add,modifier=Modifier.fillMaxWidth()){Text("+ Inserisci operazione")}}
 }
}

'''
s = s[:start] + dashboard + s[end:]
main.write_text(s)
