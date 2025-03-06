package app.notification.service;

import app.notification.client.NotificationClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class TestInit implements ApplicationRunner {

    private final NotificationClient notificationClient;

    @Autowired
    public TestInit(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
//
//        ResponseEntity<String> response = notificationClient.getHelloMessage("Vik");
//
//        System.out.println(response.getBody());
    }
}
