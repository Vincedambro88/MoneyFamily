from pathlib import Path

p = Path('app/src/main/java/com/moneyfamily/app/MainActivity.kt')
s = p.read_text()

# Existing movements: use the persisted real tipologia when editing.
s = s.replace('var type by remember(old){mutableStateOf(old?.type?.name?:"")}', 'var type by remember(old){mutableStateOf(old?.typeName?.takeIf{it.isNotBlank() && !it.equals("EXPENSE",true)}?:"")}')

# Never expose the technical MovementType value as a dashboard tipologia.
s = s.replace('val typeTotals=cur.groupBy{it.typeName.ifBlank{"Non classificata"}}.mapValues{(_,v)->v.sumOf{it.amount}}.filterValues{it!=0.0}.toList().sortedBy{it.second}', 'val typeTotals=cur.groupBy{it.typeName.takeIf{name->name.isNotBlank()&&!name.equals("EXPENSE",true)&&!name.equals("INCOME",true)}?:"Da classificare"}.mapValues{(_,v)->v.sumOf{it.amount}}.filterValues{it!=0.0}.toList().sortedBy{it.second}')

start = s.index('@Composable private fun MetricCard(')
end = s.index('@Composable private fun Operations(', start)
modern = r'''@Composable private fun MetricCard(title:String,value:Double,modifier:Modifier){
 Card(modifier,shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){Column(Modifier.padding(horizontal=14.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(title,style=MaterialTheme.typography.labelLarge);Text(money.format(value),style=MaterialTheme.typography.titleMedium,color=if(value<0)NegativeColor else PositiveColor)}}
}

@Composable private fun PieChartCard(title:String,values:List<Pair<String,Double>>){
 val positive=values.sumOf{if(it.second>0)it.second else 0.0};val negative=values.sumOf{if(it.second<0)abs(it.second) else 0.0};val total=positive+negative
 Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(title,style=MaterialTheme.typography.titleLarge);if(total==0.0)Text("Nessun movimento nel mese")else Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(150.dp),contentAlignment=Alignment.Center){Canvas(Modifier.fillMaxSize().padding(8.dp)){var start=0f;if(positive>0){val sweep=(positive/total*360.0).toFloat();drawArc(PositiveColor,start,sweep,false,style=androidx.compose.ui.graphics.drawscope.Stroke(width=30f,cap=androidx.compose.ui.graphics.StrokeCap.Round));start+=sweep};if(negative>0){val sweep=(negative/total*360.0).toFloat();drawArc(NegativeColor,start,sweep,false,style=androidx.compose.ui.graphics.drawscope.Stroke(width=30f,cap=androidx.compose.ui.graphics.StrokeCap.Round))}};Column(horizontalAlignment=Alignment.CenterHorizontally){Text(money.format(positive-negative),style=MaterialTheme.typography.titleMedium);Text("saldo",style=MaterialTheme.typography.labelSmall)}};Spacer(Modifier.width(18.dp));Column(verticalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.weight(1f)){if(positive>0)LegendRow("Entrate",positive,PositiveColor);if(negative>0)LegendRow("Uscite",-negative,NegativeColor)}}}}
}

@Composable private fun LegendRow(name:String,value:Double,color:androidx.compose.ui.graphics.Color){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){Box(Modifier.size(11.dp).clip(RoundedCornerShape(50)).background(color));Column{Text(name,style=MaterialTheme.typography.labelLarge);Text(money.format(value),style=MaterialTheme.typography.bodyMedium,color=color)}}}

@Composable private fun BarChartCard(title:String,values:List<Pair<String,Double>>){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(11.dp)){Text(title,style=MaterialTheme.typography.titleLarge);if(values.isEmpty())Text("Nessun dato per il mese")else{val maxAbs=values.maxOf{abs(it.second)}.coerceAtLeast(1.0);values.take(12).forEach{(n,v)->BarChartRow(n,v,maxAbs)}}}}}

@Composable private fun BarChartRow(name:String,value:Double,maxAbs:Double){val fraction=(abs(value)/maxAbs).toFloat().coerceIn(0f,1f);val color=if(value<0)NegativeColor else PositiveColor;Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(5.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(name,Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge);Text(money.format(value),color=color,style=MaterialTheme.typography.bodyLarge)};Box(Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(11.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha=.55f))){if(fraction>0)Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(11.dp)).background(color))}}}

'''
s = s[:start] + modern + s[end:]
p.write_text(s)
