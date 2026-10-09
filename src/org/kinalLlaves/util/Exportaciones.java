package org.kinalllaves.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;


public final class Exportaciones {

    private Exportaciones() {
    }

    private static String xml(String t) {
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    public static void xlsx(Path archivo, List<Map<String, Object>> rows) throws IOException {
        if (rows.isEmpty()) {
            throw new IOException("No hay datos para exportar");
        }
        List<String> cols = new ArrayList<>(rows.get(0).keySet());
        try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(archivo))) {
            entry(z, "[Content_Types].xml", "<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/></Types>");
            entry(z, "_rels/.rels", "<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            entry(z, "xl/workbook.xml", "<?xml version=\"1.0\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"KinalLlaves\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            entry(z, "xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>");
            entry(z, "xl/styles.xml", "<?xml version=\"1.0\"?><styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><fonts count=\"1\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts><fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills><borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs><cellXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/></cellXfs></styleSheet>");
            StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
            int r = 1;
            xml.append("<row r=\"1\">");
            for (int i = 0; i < cols.size(); i++) {
                xml.append(cell(cols.get(i), r, i));
            }
            xml.append("</row>");
            for (Map<String, Object> row : rows) {
                xml.append("<row r=\"").append(++r).append("\">");
                for (int i = 0; i < cols.size(); i++) {
                    xml.append(cell(Objects.toString(row.get(cols.get(i)), ""), r, i));
                }
                xml.append("</row>");
            }
            xml.append("</sheetData></worksheet>");
            entry(z, "xl/worksheets/sheet1.xml", xml.toString());
        }
    }

    private static String cell(String s, int row, int col) {
        int n = col + 1;
        String name = "";
        while (n > 0) {
            name = (char) ('A' + (n - 1) % 26) + name;
            n = (n - 1) / 26;
        }
        return "<c r=\"" + name + row + "\" t=\"inlineStr\"><is><t xml:space=\"preserve\">" + xml(s) + "</t></is></c>";
    }

    private static void entry(ZipOutputStream z, String name, String text) throws IOException {
        z.putNextEntry(new ZipEntry(name));
        z.write(text.getBytes(StandardCharsets.UTF_8));
        z.closeEntry();
    }

    public static void pdf(Path archivo, String titulo, List<Map<String, Object>> rows) throws IOException {
        if (rows.isEmpty()) {
            throw new IOException("No hay datos para exportar");
        }
        List<String> cols = new ArrayList<>(rows.get(0).keySet());
        List<String> lines = new ArrayList<>();
        lines.add(titulo);
        lines.add("KinalLlaves - Sistema de control de llaves");
        lines.add("");
        lines.add(String.join(" | ", cols));
        for (Map<String, Object> row : rows) {
            List<String> vals = new ArrayList<>();
            for (String col : cols) {
                vals.add(Objects.toString(row.get(col), ""));
            }
            lines.add(String.join(" | ", vals));
        }
        // Helvetica/WinAnsi; el contenido se translitera de forma segura para exportar informes sencillos.
        List<List<String>> pages = new ArrayList<>();
        for (int i = 0; i < lines.size(); i += 42) {
            pages.add(lines.subList(i, Math.min(lines.size(), i + 42)));
        }
        int pageCount = pages.size();
        int base = 3, firstContent = base + 2 * pageCount;
        List<byte[]> object = new ArrayList<>();
        object.add(null);
        object.add("<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.ISO_8859_1));
        StringBuilder kids = new StringBuilder();
        for (int p = 0; p < pageCount; p++) {
            kids.append((base + p * 2)).append(" 0 R ");
        }
        object.add(("<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>").getBytes(StandardCharsets.ISO_8859_1));
        for (int p = 0; p < pageCount; p++) {
            int pid = base + p * 2;
            int cid = pid + 1;
            object.add(("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 " + firstContent + " 0 R >> >> /Contents " + cid + " 0 R >>").getBytes(StandardCharsets.ISO_8859_1));
            StringBuilder content = new StringBuilder("BT /F1 9 Tf 38 802 Td 13 TL ");
            for (String line : pages.get(p)) {
                content.append("(").append(pdfEscape(line.length() > 118 ? line.substring(0, 118) : line)).append(") Tj T* ");
            }
            content.append("ET");
            byte[] stream = content.toString().getBytes(StandardCharsets.ISO_8859_1);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            buf.write(("<< /Length " + stream.length + " >>\nstream\n").getBytes(StandardCharsets.ISO_8859_1));
            buf.write(stream);
            buf.write("\nendstream".getBytes(StandardCharsets.ISO_8859_1));
            object.add(buf.toByteArray());
        }
        object.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>".getBytes(StandardCharsets.ISO_8859_1));
        try (OutputStream out = Files.newOutputStream(archivo)) {
            out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
            List<Integer> offsets = new ArrayList<>();
            offsets.add(0);
            int length = 9;
            for (int i = 1; i < object.size(); i++) {
                offsets.add(length);
                byte[] obj = (i + " 0 obj\n").getBytes(StandardCharsets.ISO_8859_1);
                out.write(obj);
                length += obj.length;
                out.write(object.get(i));
         