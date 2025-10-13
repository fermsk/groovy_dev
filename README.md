# how to use the DSL:
```
def builder = new XlsxBuilder("test.xlsx").with {
    sheet(idx: 0) {
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
    build()
}
```
