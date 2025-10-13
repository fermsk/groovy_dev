class Event {
    String message
    LocalDateTime dateTime

    Event(String message, LocalDateTime dateTime) {
        this.message = message
        this.dateTime = dateTime
    }
}