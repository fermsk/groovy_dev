def excel = new XlsxBuilder("report")

excel.with {
    // Create a sheet named "Sales Report"
    sheet(name: "Sales Report") {
        // Header row with styled cells
        row(idx: 0) {
            cell {
                value = "Product"
                style = new Style(backgroundColor: "GREY_25_PERCENT", fontColor: "BLUE")
            }
            cell {
                value = "Quantity"
                style = new Style(backgroundColor: "GREY_25_PERCENT", fontColor: "BLUE")
            }
            cell {
                value = "Price"
                style = new Style(backgroundColor: "GREY_25_PERCENT", fontColor: "BLUE")
            }
        }

        // Data rows
        row(idx: 1) {
            cell { value = "Laptop" }
            cell { value = 5 }
            cell {
                value = "999.99"
                style = new Style(fontColor: "RED")
            }
        }

        row(idx: 2) {
            cell { value = "Mouse" }
            cell { value = 10 }
            cell {
                value = "24.99"
                style = new Style(fontColor: "RED")
            }
        }

        // Totals row with different style
        row(idx: 3) {
            cell {
                value = "Total"
                style = new Style(backgroundColor: "YELLOW", fontColor: "BLACK")
            }
            cell { value = "15" }
            cell {
                value = "1,249.89"
                style = new Style(backgroundColor: "YELLOW", fontColor: "RED")
            }
        }
    }

    // Create another sheet for additional data
    sheet(name: "Inventory") {
        row(idx: 0) {
            cell {
                value = "Stock Report"
                style = new Style(backgroundColor: "LIGHT_BLUE", fontColor: "WHITE")
            }
        }

        row(idx: 1) {
            cell { value = "Product" }
            cell { value = "In Stock" }
        }

        row(idx: 2) {
            cell { value = "Laptop" }
            cell {
                value = "Low Stock"
                style = new Style(fontColor: "RED")
            }
        }
    }

    // Generate the Excel file
    build()
}