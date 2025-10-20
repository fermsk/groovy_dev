@Grab(group='org.apache.poi',  module='poi-ooxml', version='5.2.3')

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
            return sheet(*args)
        }
    }
    
    def sheet(Map params = [:], Closure closure) {
        def idx = params.idx ?: 0
        def name = params.name ?: idx.toString()
        
        currentSheet = workbook.createSheet(name)
        if (closure) {
            closure.delegate = this
            closure.call()
        }
    }
    
    def row(Map params = [:], Closure closure) {
        def idx = params.idx ?: 0
        currentRow = currentSheet.createRow(idx)
        
        if (closure) {
            closure.delegate = this
            closure.call()
        }
    }
    
    def cell(Map params = [:], Closure closure) {
        def cellConfig = new CellConfig()
        
        if (closure) {
            closure.delegate = cellConfig
            closure.resolveStrategy = Closure.DELEGATE_FIRST
            closure.call()
        }
        
        def cellIdx = cellConfig.idx ?: (currentRow.getLastCellNum() == -1 ? 0 : currentRow.getLastCellNum())
        def cell = currentRow.createCell(cellIdx)
        
        switch (cellConfig.value) {
            case Integer:
            case Long:
                cell.setCellValue(cellConfig.value as double)
                break
            case Boolean:
                cell.setCellValue(cellConfig.value as boolean)
                break
            default:
                cell.setCellValue(cellConfig.value?.toString())
        }
        
        if (cellConfig.style) {
            applyStyle(cell, cellConfig.style)
        }
    }
    
    private void applyStyle(XSSFCell cell, Style style) {
        def cellStyle = workbook.createCellStyle()
        
        if (style.backgroundColor) {
            cellStyle.setFillForegroundColor(style.backgroundColor)
            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)
        }
        
        if (style.fontColor || style.bold) {
            def font = workbook.createFont()
            if (style.fontColor) {
                font.setColor(style.fontColor)
            }
            if (style.bold) {
                font.setBold(true)
            }
            cellStyle.setFont(font)
        }
        
        cell.setCellStyle(cellStyle)
    }
    
    def build() {
        new FileOutputStream(filename).withCloseable { os ->
            workbook.write(os)
        }
        workbook.close()
    }
}

class CellConfig {
    def idx
    def value
    Style style
    
    def propertyMissing(String name, value) {
        this[name] = value
    }
}

class Style {
    short backgroundColor
    short fontColor
    boolean bold = false
    
    Style(Map params = [:]) {
        params.each { key, value ->
            this[key] = value
        }
    }
}
 
