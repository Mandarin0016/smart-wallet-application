package app.notification.service;

import app.notification.client.NotificationClient;
import app.notification.client.dto.Notification;
import app.notification.client.dto.NotificationPreference;
import app.notification.client.dto.NotificationRequest;
import app.notification.client.dto.UpsertNotificationPreference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    private final NotificationClient notificationClient;

    @Autowired
    public NotificationService(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    public void saveNotificationPreference(UUID userId, boolean isEmailEnabled, String email) {

        UpsertNotificationPreference notificationPreference = UpsertNotificationPreference.builder()
                .userId(userId)
                .contactInfo(email)
                .type("EMAIL")
                .notificationEnabled(isEmailEnabled)
                .build();

        // Invoke Feign client and execute HTTP Post Request.
        try {
            ResponseEntity<Void> httpResponse = notificationClient.upsertNotificationPreference(notificationPreference);
            if (!httpResponse.getStatusCode().is2xxSuccessful()) {
                log.error("[Feign call to notification-svc failed] Can't save user preference for user with id = [%s]".formatted(userId));
            }
        } catch (Exception e) {
            log.error("Unable to call notification-svc.");
        }
    }

    public NotificationPreference getNotificationPreference(UUID userId) {

        ResponseEntity<NotificationPreference> httpResponse = notificationClient.getUserPreference(userId);

        if (!httpResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Notification preference for user id [%s] does not exist.".formatted(userId));
        }

        return httpResponse.getBody();
    }

    public List<Notification> getNotificationHistory(UUID userId) {

        ResponseEntity<List<Notification>> httpResponse = notificationClient.getNotificationHistory(userId);

        return httpResponse.getBody();
    }

    public void sendNotification(UUID userId, String emailSubject, String emailBody) {

        NotificationRequest notificationRequest = NotificationRequest.builder()
                .userId(userId)
                .subject(emailSubject)
                .body(emailBody)
                .build();

        // Servive to Service
        ResponseEntity<Void> httpResponse;
        try {
            httpResponse = notificationClient.sendNotification(notificationRequest);
            if (!httpResponse.getStatusCode().is2xxSuccessful()) {
                log.error("[Feign call to notification-svc failed] Can't send email to user with id = [%s]".formatted(userId));
            }
        } catch (Exception e) {
            log.warn("Can't send email to user with id = [%s] due to 500 Internal Server Error.".formatted(userId));
        }
    }

    public void updateNotificationPreference(UUID userId, boolean enabled) {

        try {
            notificationClient.updateNotificationPreference(userId, enabled);
        } catch (Exception e) {
            log.warn("Can't update notification preferences for user with id = [%s].".formatted(userId));
        }
    }
}
