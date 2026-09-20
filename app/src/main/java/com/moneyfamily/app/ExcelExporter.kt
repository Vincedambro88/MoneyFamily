package com.moneyfamily.app

import android.content.Context
import android.net.Uri
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelExporter {
    fun write(context: Context, uri: Uri, rows: List<UiMovement>) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            ZipOutputStream(out).use { zip ->
                put(zip, "[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""")
                put(zip, "_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
                put(zip, "xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Operazioni" sheetId="1" r:id="rId1"/></sheets></workbook>""")
                put(zip, "xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""")
                val table = buildList {
                    add(listOf("Data", "Importo", "Tipologia", "Categoria", "Componente", "Descrizione"))
                    rows.sortedBy { parse(it.date)?.timeInMillis ?: Long.MAX_VALUE }.forEach {
                        add(listOf(it.date, it.amount.toString().replace(".", ","), it.typeName, it.category, it.member, it.description))
                    }
                }
                put(zip, "xl/worksheets/sheet1.xml", sheet(table))
            }
        } ?: error("Impossibile creare il file Excel")
    }

    private fun put(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun sheet(rows: List<List<String>>): String {
        fun esc(v: String) = v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace(""", "&quot;")
        fun col(n: Int): String { var x = n + 1; var s = ""; while (x > 0) { val r = (x - 1) % 26; s = ('A'.code + r).toChar() + s; x = (x - 1) / 26 }; return s }
        return buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
            rows.forEachIndexed { r, row ->
                append("<row r=\"").append(r + 1).append("\">")
                row.forEachIndexed { c, value -> append("<c r=\"").append(col(c)).append(r + 1).append("\" t=\"inlineStr\"><is><t>").append(esc(value)).append("</t></is></c>") }
                append("</row>")
            }
            append("</sheetData></worksheet>")
        }
    }
}
