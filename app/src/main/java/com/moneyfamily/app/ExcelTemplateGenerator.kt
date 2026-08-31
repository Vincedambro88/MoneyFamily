package com.moneyfamily.app

import android.content.Context
import android.net.Uri
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelTemplateGenerator {
    fun write(context: Context, uri: Uri) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            ZipOutputStream(out).use { zip ->
                put(zip, "[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>""")
                put(zip, "_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
                put(zip, "xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Operazioni" sheetId="1" r:id="rId1"/><sheet name="Istruzioni" sheetId="2" r:id="rId2"/></sheets></workbook>""")
                put(zip, "xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>""")
                put(zip, "xl/styles.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellXfs></styleSheet>""")
                val rows = listOf(
                    listOf("Data", "Importo", "Tipologia", "Categoria", "Componente", "Descrizione"),
                    listOf("01/01/2026", "-45,50", "Spesa", "Alimentari", "Papà", "Supermercato"),
                    listOf("05/01/2026", "5700,00", "Entrata", "Stipendio", "Papà", "Stipendio mensile")
                )
                put(zip, "xl/worksheets/sheet1.xml", sheet(rows))
                put(zip, "xl/worksheets/sheet2.xml", sheet(listOf(
                    listOf("Campo", "Regola"),
                    listOf("Data", "GG/MM/AAAA"),
                    listOf("Importo", "Numero positivo per entrate, negativo per spese"),
                    listOf("Tipologia", "Nome della tipologia configurata nell'app"),
                    listOf("Categoria", "Nome della categoria configurata nell'app"),
                    listOf("Componente", "Nome del membro della famiglia configurato nell'app"),
                    listOf("Descrizione", "Testo libero")
                )))
            }
        } ?: error("Impossibile creare il file Excel")
    }

    private fun put(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun sheet(rows: List<List<String>>): String {
        fun esc(v: String) = v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
        fun col(n: Int): String { var x=n+1; var s=""; while(x>0){val r=(x-1)%26;s=('A'.code+r).toChar()+s;x=(x-1)/26};return s }
        return buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
            rows.forEachIndexed { r, row ->
                append("<row r=\"").append(r+1).append("\">")
                row.forEachIndexed { c, value -> append("<c r=\"").append(col(c)).append(r+1).append("\" t=\"inlineStr\"><is><t>").append(esc(value)).append("</t></is></c>") }
                append("</row>")
            }
            append("</sheetData></worksheet>")
        }
    }
}
