import grails.gorm.annotation.Entity
import org.grails.datastore.gorm.GormEntity

@Entity
class Task implements GormEntity {
    String name
    LocalDateTime startTime
    LocalDateTime endTime
    static hasMany = [actions: Action]

    static constraints = {
        name blank: false
        startTime nullable: false
        endTime nullable: false
        actions nullable: true
    }

    static mapping = {
        actions cascade: 'all-delete-orphan'
    }
}