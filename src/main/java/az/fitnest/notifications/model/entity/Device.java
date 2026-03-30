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
        @Index(name = "idx_devices_user_id", columnList = "user_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_devices_user_device", columnNames = {"user_id", "device_id"})
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

    @Column(name = "push_token")
    private String pushToken;

    @Column(name = "platform", nullable = false)
    @Enumerated(EnumType.STRING)
    private Platform platform;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "notification_enabled", nullable = false)
    private Boolean notificationEnabled = true;

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = false;
}
