package az.fitnest.notifications.service.impl;

import az.fitnest.notifications.dto.NotificationTemplateResponse;
import az.fitnest.notifications.model.enums.NotificationChannel;
import az.fitnest.notifications.service.NotificationTemplateService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    private final List<NotificationTemplateResponse> templates;

    public NotificationTemplateServiceImpl(ObjectMapper objectMapper) {
        this.templates = loadTemplates(objectMapper);
    }

    @Override
    public List<NotificationTemplateResponse> getTemplates(NotificationChannel channel) {
        if (channel == null) return templates;
        return templates.stream().filter(t -> Objects.equals(t.channel(), channel)).toList();
    }

    private List<NotificationTemplateResponse> loadTemplates(ObjectMapper objectMapper) {
        try {
            ClassPathResource resource = new ClassPathResource("notification-templates.json");
            try (InputStream is = resource.getInputStream()) {
                List<NotificationTemplateResponse> list =
                        objectMapper.readValue(is, new TypeReference<List<NotificationTemplateResponse>>() {});
                return list != null ? List.copyOf(list) : Collections.emptyList();
            }
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}

