package az.fitnest.notifications.repository;

import az.fitnest.notifications.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    List<Device> findAllByUserId(Long userId);
    Optional<Device> findByPushToken(String pushToken);
    void deleteByPushToken(String pushToken);
}
