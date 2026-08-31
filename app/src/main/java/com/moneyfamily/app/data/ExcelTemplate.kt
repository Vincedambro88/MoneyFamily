package com.moneyfamily.app.data

import android.content.Context
import android.net.Uri
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Creates a minimal valid XLSX workbook without external spreadsheet libraries. */
object ExcelTemplate {
    fun write(context: Context, uri: Uri) {
        val entries = linkedMapOf(
            "[Content_Types].xml" to """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""",
            "_rels/.rels" to """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""",
            "xl/_rels/workbook.xml.rels" to """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""",
            "xl/workbook.xml" to """<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Movimenti" sheetId="1" r:id="rId1"/></sheets></workbook>""",
            "xl/worksheets/sheet1.xml" to """<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData><row r="1"><c r="A1" t="inlineStr"><is><t>Data</t></is></c><c r="B1" t="inlineStr"><is><t>Importo</t></is></c><c r="C1" t="inlineStr"><is><t>Tipologia</t></is></c><c r="D1" t="inlineStr"><is><t>Categoria</t></is></c><c r="E1" t="inlineStr"><is><t>Componente</t></is></c><c r="F1" t="inlineStr"><is><t>Descrizione</t></is></c></row><row r="2"><c r="A2" t="inlineStr"><is><t>31/08/2026</t></is></c><c r="B2" t="n"><v>-10.00</v></c><c r="C2" t="inlineStr"><is><t>Spesa</t></is></c><c r="D2" t="inlineStr"><is><t>Categoria</t></is></c><c r="E2" t="inlineStr"><is><t>Componente</t></is></c><c r="F2" t="inlineStr"><is><t>Esempio</t></is></c></row></sheetData></worksheet>"""
        )
        context.contentResolver.openOutputStream(uri)?.use { output ->
            ZipOutputStream(output).use { zip ->
                entries.forEach { (name, content) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(content.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            }
        } ?: error("Impossibile aprire il file destinazione")
    }
}
