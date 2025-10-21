import  org.apache.poi.xssf.usermodel.*
import org.apache.poi.ss.usermodel.*
import groovy.test.GroovyTestCase

class XlsxBuilderTest extends GroovyTestCase {
    
    void testBasicXlsxCreation() {
        def builder = new XlsxBuilder("test.xlsx")
        
        builder.sheet(idx: 0) {
            row(idx: 0) {
                cell {
                    value = 1
                    style = new Style(backgroundColor: IndexedColors.YELLOW.index)
                }
                cell {
                    idx = 3
                    value = "test"
                    style = new Style(fontColor: IndexedColors.RED.index, bold: true)
                }
            }
        }
        
        builder.build()
        
        // Verify file exists
        def file = new File("test.xlsx")
        assertTrue("Excel file should be created", file.exists())
        
        // Verify content
        def workbook = new XSSFWorkbook(new FileInputStream("test.xlsx"))
        def sheet = workbook.getSheetAt(0)
        def row = sheet.getRow(0)
        
        assertEquals("First cell value should be 1", 1.0, row.getCell(0).getNumericCellValue(), 0.01)
        assertEquals("Fourth cell value should be 'test'", "test", row.getCell(3).getStringCellValue())
        
        workbook.close()
        file.delete()
    }
}
 
