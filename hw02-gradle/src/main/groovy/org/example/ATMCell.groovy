class ATMCell {
    final Banknote banknote
    private int count
    
    ATMCell(Banknote banknote) {
        this.banknote = banknote
        this.count = 0
    }
    
    void add(int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("Quantity cannot be negative")
        count += quantity
    }
    
    boolean withdraw(int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("Quantity cannot be negative")
        if (quantity > count) return false
        count -= quantity
        return true
    }
    
    int getCount() { return count }
    
    int getTotal() { return banknote.denomination * count }
}
