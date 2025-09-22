class Period {
    LocalDateTime start
    LocalDateTime end

    Period(LocalDateTime start, LocalDateTime end) {
        this.start = start
        this.end = end
    }

    String toString() {
        "From: ${start} To: ${end}"
    }
}
