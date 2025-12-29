import grails.gorm.annotation.Entity
import org.grails.datastore.gorm.GormEntity

@Entity
class Action implements GormEntity {
    String name
    LocalDateTime startTime
    LocalDateTime endTime
    static belongsTo = [task: Task]

    static constraints = {
        name blank: false
        startTime nullable: false
        endTime nullable: false
        task nullable: false
    }

    static mapping = {
        task cascade: 'none'
    }
}