def main() {
    // Create ATM with supported denominations
    def atm = new ATM([5000, 1000, 500, 100])
    
    // Deposit money using operator overloading (<<)
    atm << [5000:10, 1000:20, 500:10, 100:50]
    
    println "Initial state:"
    println atm
    println "\nTotal balance: ₽${atm.balance}"
    
    try {
        // Try to withdraw money
        def withdrawal = atm.withdraw(13700)
        println "\nWithdrawn amount: ₽13700"
        println "Used banknotes:"
        withdrawal.each { denomination, count ->
            println "₽${denomination}: ${count} notes"
        }
        
        println "\nATM state after withdrawal:"
        println atm
        
    } catch (Exception e) {
        println "\nError: ${e.message}"
    }
}

// Run the example
main()
