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

    @Query("SELECT d.pushToken FROM Device d WHERE d.userId = :userId AND d.notificationEnabled = true")
    List<String> findPushTokensByUserId(@Param("userId") Long userId);

    @Query("SELECT d.pushToken FROM Device d WHERE d.notificationEnabled = true")
    List<String> findAllPushTokens();

    Optional<Device> findByPushToken(String pushToken);

    void deleteByPushToken(String pushToken);

    void deleteByUserId(Long userId);

    long countByUserId(Long userId);

    @Modifying
    @Query("UPDATE Device d SET d.isCurrent = false, d.notificationEnabled = false WHERE d.userId = :userId AND d.pushToken != :pushToken")
    void deactivateOtherDevices(@Param("userId") Long userId, @Param("pushToken") String pushToken);
}
