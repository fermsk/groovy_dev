class Banknote implements Comparable<Banknote> {
    final int denomination
    
    Banknote(int denomination) {
        this.denomination = denomination
    }
    
    @Override
    int compareTo(Banknote other) {
        return other.denomination <=> this.denomination // Descending order
    }
    
    @Override
    String toString() {
        return "₽${denomination}"
    }
    
    // Operator overloading for addition
    def plus(Banknote other) {
        return this.denomination + other.denomination
    }
}
