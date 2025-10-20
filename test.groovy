@Grab(group='org.apache.poi',  module='poi-ooxml', version='5.2.3')

import org.apache.poi.xssf.usermodel.*
import org.apache.poi.ss.usermodel.*

// Include the XlsxBuilder classes
evaluate(new File('XlsxBuilder.groovy'))

// Test the DSL
def builder = new XlsxBuilder("test.xlsx")

builder.sheet(idx: 0) {
    row(idx: 0) {
        cell {
            value = 1
            style = new Style()
        }
        cell {
            idx = 3
            value = "test"
            style = new Style()
        }
    }
}

builder.build()

// Verify the generated file
def workbook = new XSSFWorkbook(new FileInputStream("test.xlsx"))
def sheet = workbook.getSheetAt(0)
def row = sheet.getRow(0)

def cell0 = row.getCell(0)
def cell3 = row.getCell(3)

assert cell0.getNumericCellValue() == 1.0
assert cell3.getStringCellValue() == "test"

workbook.close()

println "✅ All tests passed!"
new File("test-output.txt").text = "Build successful - XLSX file generated correctly"
