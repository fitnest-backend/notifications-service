package az.fitnest.notifications.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_counters", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_counters_user_id", columnNames = {"user_id"})
})
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
