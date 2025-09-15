class ATM {
    private final Map<Integer, ATMCell> cells = new TreeMap<Integer, ATMCell>().descendingMap()
    
    ATM(List<Integer> denominations) {
        denominations.each { denomination ->
            cells[denomination] = new ATMCell(new Banknote(denomination))
        }
    }
    
    // Operator overloading for adding money to ATM
    def leftShift(Map<Integer, Integer> deposit) {
        deposit.each { denomination, count ->
            if (!cells.containsKey(denomination)) {
                throw new IllegalArgumentException("Unsupported denomination: ₽$denomination")
            }
            cells[denomination].add(count)
        }
        return this
    }
    
    Map<Integer, Integer> withdraw(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive")
        if (amount > balance) throw new IllegalStateException("Insufficient funds")
        
        Map<Integer, Integer> result = [:]
        int remainingAmount = amount
        
        // First pass: Try to calculate the optimal distribution
        cells.each { denomination, cell ->
            int neededNotes = remainingAmount.intdiv(denomination)
            if (neededNotes > 0) {
                int availableNotes = Math.min(neededNotes, cell.count)
                if (availableNotes > 0) {
                    result[denomination] = availableNotes
                    remainingAmount -= denomination * availableNotes
                }
            }
        }
        
        // Check if we can fulfill the request
        if (remainingAmount > 0) {
            throw new IllegalStateException("Cannot dispense exact amount")
        }
        
        // Second pass: Actually withdraw the money
        result.each { denomination, count ->
            cells[denomination].withdraw(count)
        }
        
        return result
    }
    
    int getBalance() {
        return cells.values().sum { it.total }
    }
    
    @Override
    String toString() {
        return "ATM Balance: ₽${balance}\n" + 
               cells.collect { denomination, cell ->
                   "₽${denomination}: ${cell.count} notes"
               }.join('\n')
    }
}
