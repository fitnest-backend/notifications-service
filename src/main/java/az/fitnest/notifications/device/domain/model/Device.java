package az.fitnest.notifications.device.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Device {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long deviceId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "push_token")
    private String pushToken;
    
    @Column(name = "platform", nullable = false)
    @Enumerated(EnumType.STRING)
    private Platform platform;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    public enum Platform {
        IOS, ANDROID
    }
}
