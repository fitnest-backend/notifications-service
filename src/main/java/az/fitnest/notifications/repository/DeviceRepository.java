package az.fitnest.notifications.repository;

import az.fitnest.notifications.model.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    List<Device> findAllByUserId(Long userId);

    Optional<Device> findFirstByUserIdAndIsCurrentTrue(Long userId);

    Optional<Device> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT d.pushToken FROM Device d WHERE d.userId = :userId AND d.isCurrent = true AND d.notificationEnabled = true")
    List<String> findPushTokensByUserId(@Param("userId") Long userId);

    @Query("SELECT d.pushToken FROM Device d WHERE d.isCurrent = true AND d.notificationEnabled = true")
    List<String> findAllPushTokens();

    @Query("SELECT DISTINCT d.userId FROM Device d WHERE d.isCurrent = true AND d.notificationEnabled = true")
    List<Long> findUserIdsWithActivePushEnabled();

    Optional<Device> findByPushToken(String pushToken);

    @Query("SELECT d FROM Device d WHERE d.pushToken = :pushToken ORDER BY d.createdAt DESC, d.deviceId DESC")
    List<Device> findAllByPushTokenOrderByNewest(@Param("pushToken") String pushToken);

    void deleteByPushToken(String pushToken);

    void deleteByUserId(Long userId);

    long countByUserId(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Device d SET d.isCurrent = false, d.notificationEnabled = false WHERE d.userId = :userId AND d.pushToken <> :pushToken")
    int deactivateOtherDevices(@Param("userId") Long userId, @Param("pushToken") String pushToken);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Device d SET d.isCurrent = false WHERE d.userId = :userId AND d.isCurrent = true")
    int clearCurrentFlag(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Device d SET d.isCurrent = false, d.notificationEnabled = false WHERE d.userId = :userId")
    int disableAllDevicesForUser(@Param("userId") Long userId);
}
