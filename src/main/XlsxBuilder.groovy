@Grab(group='org.apache.poi', module='poi-ooxml', version='5.2.3')

import org.apache.poi.xssf.usermodel.*
import org.apache.poi.ss.usermodel.*

class XlsxBuilder {
    private XSSFWorkbook workbook
    private String filename
    private XSSFSheet currentSheet
    private XSSFRow currentRow

    XlsxBuilder(String filename) {
        this.filename = filename.endsWith('.xlsx') ? filename : "${filename}.xlsx"
        this.workbook = new XSSFWorkbook()
    }

    def methodMissing(String name, args) {
        if (name == 'sheet') {
            sheet(*args)
        }
    }

    def sheet(Map params = [:], Closure closure) {
        def idx = params.idx ?: 0
        def name = params.name ?: idx.toString()
        
        currentSheet = workbook.createSheet(name)
        if (closure) {
            closure.delegate = this
            closure.resolveStrategy = Closure.DELEGATE_FIRST
            closure()
        }
    }

    def row(Map params = [:], Closure closure) {
        def idx = params.idx ?: 0
        currentRow = currentSheet.createRow(idx)
        
        if (closure) {
            closure.delegate = this
            closure.resolveStrategy = Closure.DELEGATE_FIRST
            closure()
        }
    }

    def cell(Map params = [:], Closure closure) {
        def cellBuilder = new CellBuilder(currentRow)
        
        if (closure) {
            closure.delegate = cellBuilder
            closure.resolveStrategy = Closure.DELEGATE_FIRST
            closure()
        }
        
        cellBuilder.build()
    }

    def build() {
        new File(filename).withOutputStream { out ->
            workbook.write(out)
        }
        workbook.close()
    }
}

class Style {
    String backgroundColor  // For cell background
    String fontColor       // For text color
    
    Style(Map params = [:]) {
        backgroundColor = params.backgroundColor
        fontColor = params.fontColor
    }
}

class CellBuilder {
    private XSSFRow row
    Integer idx
    def value
    Style style

    CellBuilder(XSSFRow row) {
        this.row = row
        this.idx = row.lastCellNum == -1 ? 0 : row.lastCellNum
    }

    def build() {
        def cell = row.createCell(idx)
        
        switch (value) {
            case Integer:
                cell.setCellValue(value as Integer)
                break
            case Boolean:
                cell.setCellValue(value as Boolean)
                break
            default:
                cell.setCellValue(value?.toString())
        }

        if (style) {
            applyStyle(cell, style)
        }
    }

     private void applyStyle(XSSFCell cell, Style style) {
        def workbook = row.sheet.workbook
        def cellStyle = workbook.createCellStyle()
        
        // Apply background color if specified
        if (style.backgroundColor) {
            // Convert color string to IndexedColors
            def bgColor = getColorFromString(style.backgroundColor)
            cellStyle.setFillForegroundColor(bgColor.index)
            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)
        }
        
        // Apply font color if specified
        if (style.fontColor) {
            def font = workbook.createFont()
            def fontColor = getColorFromString(style.fontColor)
            font.setColor(fontColor.index)
            cellStyle.setFont(font)
        }
        
        cell.cellStyle = cellStyle
    }
    
    private IndexedColors getColorFromString(String colorName) {
        try {
            return IndexedColors.valueOf(colorName.toUpperCase())
        } catch (IllegalArgumentException e) {
            // Default to black if color not found
            return IndexedColors.BLACK
        }
    }
}
