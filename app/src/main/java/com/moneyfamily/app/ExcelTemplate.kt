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
    private fun sheet1() = """<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>${row(1,listOf("Data","Descrizione","Importo","Tipologia","Categoria","Membro famiglia"),true)}${row(2,listOf("01/01/2026","Stipendio gennaio","5700","Stipendio","Stipendio","Papà"))}${row(3,listOf("05/01/2026","Supermercato","-45.50","Spesa","Alimentari","Mamma"))}</sheetData></worksheet>"""
    private fun sheet2() = """<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>${row(1,listOf("MoneyFamily - Modello importazione operazioni"),true)}${row(2,listOf("Campi: Data, Descrizione, Importo, Tipologia, Categoria, Membro famiglia."))}${row(3,listOf("Tipologia","Deve corrispondere a una tipologia configurata nell'app."))}${row(4,listOf("Categoria","Deve corrispondere a una categoria configurata nell'app."))}${row(5,listOf("Membro famiglia","Deve corrispondere a un membro configurato nell'app."))}${row(6,listOf("Nota","Non è presente alcun campo Entrata/Uscita nel template."))}</sheetData></worksheet>"""
    private fun xml(s: String) = s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
}
