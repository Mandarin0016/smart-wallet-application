package app.notification.client.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class NotificationPreference {

    private String type;

    private boolean enabled;

    private String contactInfo;
}
