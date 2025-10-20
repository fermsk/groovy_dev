# how to use the DSL:
```
def  builder = new XlsxBuilder("styled_example")

builder.sheet(name: "Example") {
    row(idx: 0) {
        cell {
            value = "Header 1"
            style = new Style(backgroundColor: IndexedColors.YELLOW, bold: true)
        }
        cell {
            value = "Header 2"
            style = new Style(backgroundColor: IndexedColors.LIGHT_BLUE, fontColor: IndexedColors.WHITE)
        }
    }
    row(idx: 1) {
        cell { value = "Data 1" }
        cell { 
            value = "Important"
            style = new Style(fontColor: IndexedColors.RED, bold: true)
        }
    }
}

builder.build()
 
```
