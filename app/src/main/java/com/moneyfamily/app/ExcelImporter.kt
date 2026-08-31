package com.moneyfamily.app

import android.content.Context
import android.net.Uri
import com.moneyfamily.app.data.ImportRefresh
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

data class ImportedRow(val date:String,val amount:Double,val typeName:String,val category:String,val member:String,val description:String)

object ExcelImporter {
    fun import(context:Context, uri:Uri):List<ImportedRow>{
        val mime=context.contentResolver.getType(uri).orEmpty()
        val rows = if(mime.contains("spreadsheet")||mime.contains("excel")||uri.toString().lowercase().endsWith(".xlsx")) readXlsx(context,uri) else readCsv(context,uri)
        if(rows.isNotEmpty()) ImportRefresh.request()
        return rows
    }
    private fun readCsv(context:Context,uri:Uri):List<ImportedRow>{
        context.contentResolver.openInputStream(uri).use{ins->
            val lines=BufferedReader(InputStreamReader(ins!!,Charsets.UTF_8)).readLines()
            if(lines.isEmpty()) return emptyList()
            val h=lines.first().split(';',',','\t').map{norm(it)}
            validateHeaders(h)
            return lines.drop(1).filter{it.isNotBlank()}.mapIndexed{idx,line->parseCells(line.split(';',',','\t'),h,idx+2)}
        }
    }
    private fun readXlsx(context:Context,uri:Uri):List<ImportedRow>{
        val e=mutableMapOf<String,ByteArray>()
        context.contentResolver.openInputStream(uri).use{input->ZipInputStream(input!!).use{z->while(true){val x=z.nextEntry?:break;e[x.name]=z.readBytes()}}}
        val shared=e["xl/sharedStrings.xml"]?.let{shared(it)}?:emptyList()
        val bytes=e["xl/worksheets/sheet1.xml"]?:error("Foglio Excel non trovato")
        val rows=sheet(bytes,shared)
        if(rows.isEmpty()) return emptyList()
        val h=rows.first().map{norm(it)}
        validateHeaders(h)
        return rows.drop(1).mapIndexedNotNull{idx,row->if(row.all{it.isBlank()}) null else parseCells(row,h,idx+2)}
    }
    private fun validateHeaders(h:List<String>){
        val required=mapOf(
            "Data" to arrayOf("data","date"),
            "Descrizione" to arrayOf("descrizione","description"),
            "Importo" to arrayOf("importo","amount","valore"),
            "Tipologia" to arrayOf("tipologia","tipo","type"),
            "Categoria" to arrayOf("categoria","category"),
            "Membro famiglia" to arrayOf("membro famiglia","membro","componente","effettuata da","member","famiglia")
        )
        val missing=required.filter{(_,names)->h.none{header->names.any{header==norm(it)}}}.keys
        if(missing.isNotEmpty()) error("Colonne obbligatorie mancanti: ${missing.joinToString(", ")}")
    }
    private fun parseCells(cells:List<String>,h:List<String>,row:Int):ImportedRow{
        val required=mapOf(
            "data" to arrayOf("data","date"),
            "descrizione" to arrayOf("descrizione","description"),
            "importo" to arrayOf("importo","amount","valore"),
            "tipologia" to arrayOf("tipologia","tipo","type"),
            "categoria" to arrayOf("categoria","category"),
            "membro" to arrayOf("membro famiglia","membro","componente","effettuata da","member","famiglia")
        )
        val indexes=required.mapValues{(_,names)->h.indexOfFirst{a->names.any{a==norm(it)}}}
        fun get(key:String)=cells.getOrNull(indexes.getValue(key)).orEmpty().trim()
        val dateRaw=get("data");val amountRaw=get("importo");val type=get("tipologia");val category=get("categoria");val member=get("membro");val description=get("descrizione")
        if(listOf(dateRaw,amountRaw,type,category,member,description).any{it.isBlank()}) error("Riga $row: tutte le colonne sono obbligatorie e devono essere compilate")
        val date=normalizeDate(dateRaw) ?: error("Riga $row: data non valida. Usa il formato gg/MM/aaaa (es. 05/01/2026)")
        val amount=parseAmount(amountRaw) ?: error("Riga $row: importo non valido. Usa ad esempio 125,50 oppure -45,50")
        return ImportedRow(date,amount,type,category,member,description)
    }
    private fun parseAmount(raw:String):Double?{
        val s=raw.trim().replace("€","").replace(" ","")
        if(s.isBlank()) return null
        return if(s.contains(',')) s.replace(".","").replace(',','.').toDoubleOrNull() else s.toDoubleOrNull()
    }
    private fun shared(b:ByteArray):List<String>{val d=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(b.inputStream());val n=d.getElementsByTagName("si");return(0 until n.length).map{val e=n.item(it) as Element;val t=e.getElementsByTagName("t");(0 until t.length).joinToString(""){j->t.item(j).textContent}}}
    private fun sheet(b:ByteArray,shared:List<String>):List<List<String>>{
        val d=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(b.inputStream());val rs=d.getElementsByTagName("row")
        return(0 until rs.length).map{ri->val r=rs.item(ri) as Element;val cs=r.getElementsByTagName("c");val v=MutableList(32){""};for(j in 0 until cs.length){val c=cs.item(j) as Element;val col=c.getAttribute("r").takeWhile{it.isLetter()};val idx=col.fold(0){a,ch->a*26+(ch-'A'+1)}-1;val x=c.getElementsByTagName("v");val inline=c.getElementsByTagName("t");val raw=if(x.length>0)x.item(0).textContent else if(inline.length>0)inline.item(0).textContent else "";v[idx]=if(c.getAttribute("t")=="s")shared.getOrNull(raw.toIntOrNull()?:-1).orEmpty() else raw};while(v.lastOrNull().isNullOrBlank())v.removeAt(v.lastIndex);v}
    }
    private fun norm(s:String)=s.trim().lowercase(Locale.ITALIAN).replace("_"," ").replace(Regex("\\s+")," ")
    private fun normalizeDate(v:String):String?{
        val d=v.trim()
        for(pattern in listOf("dd/MM/yyyy","d/M/yyyy","dd-MM-yyyy","d-M-yyyy","yyyy-MM-dd")){
            val f=SimpleDateFormat(pattern,Locale.ITALY).apply{isLenient=false}
            runCatching{f.parse(d)?.let{return SimpleDateFormat("dd/MM/yyyy",Locale.ITALY).format(it)}}
        }
        d.toDoubleOrNull()?.let{n->if(n>20000){val c=Calendar.getInstance(TimeZone.getTimeZone("UTC"));c.timeInMillis=((n-25569)*86400000.0).toLong();return SimpleDateFormat("dd/MM/yyyy",Locale.ITALY).format(c.time)}}
        return null
    }
}
