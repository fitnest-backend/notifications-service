package az.fitnest.notifications.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures push_token uniqueness even when Hibernate ddl-auto cannot add the constraint
 * because of pre-existing duplicate rows.
 */
@Component
public class DeviceSchemaHardening {

    private static final Logger log = LoggerFactory.getLogger(DeviceSchemaHardening.class);

    private final JdbcTemplate jdbcTemplate;

    public DeviceSchemaHardening(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void hardenDeviceSchema() {
        try {
            int removed = jdbcTemplate.update("""
                    DELETE FROM devices d
                    WHERE d.push_token IS NOT NULL
                      AND d.device_id NOT IN (
                        SELECT kept_id FROM (
                          SELECT DISTINCT ON (push_token) device_id AS kept_id
                          FROM devices
                          WHERE push_token IS NOT NULL
                          ORDER BY push_token, created_at DESC NULLS LAST, device_id DESC
                        ) keepers
                      )
                    """);
            if (removed > 0) {
                log.warn("Removed {} duplicate device row(s) by push_token", removed);
            }

            jdbcTemplate.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS uk_devices_push_token ON devices (push_token)
                    """);
            log.info("Ensured unique index uk_devices_push_token on devices.push_token");
        } catch (Exception e) {
            log.error("Device schema hardening failed (non-fatal): {}", e.getMessage(), e);
        }
    }
}
