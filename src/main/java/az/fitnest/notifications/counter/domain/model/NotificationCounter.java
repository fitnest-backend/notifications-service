package az.fitnest.notifications.counter.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_counters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCounter {
    
    @Id
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "unread", nullable = false)
    private Integer unread;
}
