import grails.gorm.services.Service
import java.time.LocalDateTime

@Service(Task)
interface TaskService {
    Task save(String name, LocalDateTime startTime, LocalDateTime endTime)
    void delete(Serializable id)
    Task findById(Serializable id)
    List findAll()
    Number count()
}