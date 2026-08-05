package az.fitnest.notifications.model.entity;

import az.fitnest.notifications.model.enums.Platform;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "devices", indexes = {
        @Index(name = "idx_devices_user_id", columnList = "user_id"),
        @Index(name = "idx_devices_user_current", columnList = "user_id, is_current")
})
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

    /** FCM/APNs token — must be unique so one physical device maps to one user. */
    @Column(name = "push_token", nullable = false, unique = true, length = 512)
    private String pushToken;

    @Column(name = "platform", nullable = false)
    @Enumerated(EnumType.STRING)
    private Platform platform;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "notification_enabled", nullable = false)
    private Boolean notificationEnabled = true;

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = false;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (notificationEnabled == null) {
            notificationEnabled = true;
        }
        if (isCurrent == null) {
            isCurrent = false;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
