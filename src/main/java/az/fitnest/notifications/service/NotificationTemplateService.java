package az.fitnest.notifications.service;

import az.fitnest.notifications.dto.NotificationTemplateResponse;
import az.fitnest.notifications.model.enums.NotificationChannel;

import java.util.List;

public interface NotificationTemplateService {
    List<NotificationTemplateResponse> getTemplates(NotificationChannel channel);
}

