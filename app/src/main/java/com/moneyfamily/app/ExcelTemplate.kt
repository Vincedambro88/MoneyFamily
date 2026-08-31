package com.moneyfamily.app

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelTemplate {
    fun write(context: Context, uri: Uri) {
        val output = context.contentResolver.openOutputStream(uri)
            ?: error("Impossibile aprire il file di destinazione")
        output.use { it.write(create()) }
    }

    fun create(): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            add(zip, "[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>""")
            add(zip, "_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
            add(zip, "xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>""")
            add(zip, "xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Operazioni" sheetId="1" r:id="rId1"/><sheet name="Istruzioni" sheetId="2" r:id="rId2"/></sheets></workbook>""")
            add(zip, "xl/styles.xml", """<?xml version="1.0" encoding="UTF-8"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0"/></cellXfs></styleSheet>""")
            add(zip, "xl/worksheets/sheet1.xml", sheet1())
            add(zip, "xl/worksheets/sheet2.xml", sheet2())
        }
        return out.toByteArray()
    }

    private fun add(zip: ZipOutputStream, path: String, content: String) { zip.putNextEntry(ZipEntry(path)); zip.write(content.toByteArray(Charsets.UTF_8)); zip.closeEntry() }
    private fun cell(ref: String, value: String, style: Int = 0): String = "<c r=\"$ref\" t=\"inlineStr\" s=\"$style\"><is><t>${xml(value)}</t></is></c>"
    private fun row(n: Int, values: List<String>, header: Boolean = false): String = "<row r=\"$n\">${values.mapIndexed { i,v -> cell("${('A'.code+i).toChar()}$n",v,if(header)1 else 0) }.joinToString("")}</row>"
    private fun sheet1() = """<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>${row(1,listOf("Data","Descrizione","Importo","Tipologia","Categoria","Membro famiglia"),true)}${row(2,listOf("01/01/2026","Stipendio gennaio","5700","STIPENDIO","LAVORO","Papà"))}${row(3,listOf("05/01/2026","Supermercato","-45,50","SPESA","CASA","Mamma"))}</sheetData></worksheet>"""
    private fun sheet2() = """<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>
${row(1,listOf("MoneyFamily - Istruzioni per importazione operazioni"),true)}
${row(2,listOf("Il foglio Operazioni deve contenere esattamente questi 6 campi: Data | Descrizione | Importo | Tipologia | Categoria | Membro famiglia."))}
${row(3,listOf("Data","Data dell'operazione. Compilare in formato gg/MM/aaaa, ad esempio 05/01/2026. Sono accettati anche gg-MM-aaaa e aaaa-MM-gg."))}
${row(4,listOf("Descrizione","Testo libero che descrive l'operazione, ad esempio Supermercato o Stipendio gennaio."))}
${row(5,listOf("Importo","Importo numerico. Positivo per un ricavo/entrata e negativo per una spesa/uscita. Esempi: 5700 oppure -45,50."))}
${row(6,listOf("Tipologia","Deve corrispondere esattamente a una tipologia attiva configurata in MoneyFamily, ad esempio STIPENDIO o SPESA."))}
${row(7,listOf("Categoria","Deve corrispondere esattamente a una categoria attiva configurata in MoneyFamily, ad esempio LAVORO o CASA."))}
${row(8,listOf("Membro famiglia","Deve corrispondere al nome di un membro attivo configurato in MoneyFamily, ad esempio Papà, Mamma, Figlio 1 o Figlio 2."))}
${row(9,listOf("Entrata/Uscita","NON compilare questo campo: la colonna non esiste nel template. La natura dell'operazione viene determinata dal segno dell'Importo."))}
${row(10,listOf("Importazione","Non modificare i nomi delle colonne. Tutti i 6 campi sono obbligatori per ogni riga."))}
${row(11,listOf("Tipologia e Categoria","Se una tipologia è associata a una categoria nell'app, usa la categoria associata. Il valore deve comunque essere presente nel file."))}
${row(12,listOf("Controlli","Dopo l'importazione le operazioni vengono salvate nel database e il dashboard viene aggiornato automaticamente."))}
</sheetData></worksheet>"""
    private fun xml(s: String) = s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
}
