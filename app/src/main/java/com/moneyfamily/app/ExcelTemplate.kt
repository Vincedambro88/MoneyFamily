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
            add(zip, "[Content_Types].xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                </Types>
            """.trimIndent())
            add(zip, "_rels/.rels", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
            """.trimIndent())
            add(zip, "xl/_rels/workbook.xml.rels", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
                  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                </Relationships>
            """.trimIndent())
            add(zip, "xl/workbook.xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets><sheet name="Operazioni" sheetId="1" r:id="rId1"/><sheet name="Istruzioni" sheetId="2" r:id="rId2"/></sheets>
                </workbook>
            """.trimIndent())
            add(zip, "xl/styles.xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>
                  <fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>
                  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
                  <cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0"/></cellXfs>
                </styleSheet>
            """.trimIndent())
            add(zip, "xl/worksheets/sheet1.xml", sheet1())
            add(zip, "xl/worksheets/sheet2.xml", sheet2())
        }
        return out.toByteArray()
    }

    private fun add(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun cell(ref: String, value: String, style: Int = 0): String =
        "<c r=\"$ref\" t=\"inlineStr\" s=\"$style\"><is><t>${xml(value)}</t></is></c>"

    private fun row(n: Int, values: List<String>, header: Boolean = false): String {
        val cells = values.mapIndexed { i, v -> cell("${('A'.code + i).toChar()}$n", v, if (header) 1 else 0) }.joinToString("")
        return "<row r=\"$n\">$cells</row>"
    }

    private fun sheet1() = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>
        ${row(1, listOf("Data", "Importo", "Tipologia", "Categoria", "Componente", "Descrizione"), true)}
        ${row(2, listOf("01/01/2026", "5700", "Entrata", "Stipendio", "Papà", "Stipendio gennaio"))}
        ${row(3, listOf("05/01/2026", "-45.50", "Uscita", "Alimentari", "Mamma", "Supermercato"))}
        ${row(4, listOf("10/01/2026", "-600", "Uscita", "Casa", "Papà", "Affitto"))}
        </sheetData></worksheet>
    """.trimIndent()

    private fun sheet2() = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>
        ${row(1, listOf("MoneyFamily - Modello importazione operazioni"), true)}
        ${row(2, listOf("Compila il foglio Operazioni. Una riga corrisponde a una operazione."))}
        ${row(3, listOf("Data", "GG/MM/AAAA"))}
        ${row(4, listOf("Importo", "Numero positivo per entrate e negativo per uscite."))}
        ${row(5, listOf("Tipologia", "Entrata oppure Uscita."))}
        ${row(6, listOf("Categoria", "Nome della categoria presente in MoneyFamily."))}
        ${row(7, listOf("Componente", "Nome del membro della famiglia presente in MoneyFamily."))}
        ${row(8, listOf("Descrizione", "Descrizione dell'operazione."))}
        ${row(9, listOf("Nota", "I nomi di Categoria e Componente devono corrispondere a quelli configurati nell'app."))}
        </sheetData></worksheet>
    """.trimIndent()

    private fun xml(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
