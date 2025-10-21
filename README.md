# how to use the DSL:
```
def builder = new XlsxBuilder("styled_test.xlsx")
builder.with {
    sheet(name: "Styled Sheet") {
        row(idx: 0) {
            cell {
                value = "Red background"
                style = new Style(backgroundColor: "RED")
            }
            cell {
                value = "Blue text"
                style = new Style(fontColor: "BLUE")
            }
            cell {
                value = "Red background with green text"
                style = new Style(backgroundColor: "RED", fontColor: "GREEN")
            }
        }
    }
    build()
}
 
```
