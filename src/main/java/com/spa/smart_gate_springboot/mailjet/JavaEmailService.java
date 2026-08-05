package com.spa.smart_gate_springboot.mailjet;

import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.resource.Emailv31;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;


@Slf4j
@RequiredArgsConstructor
@Service
public class JavaEmailService {

    public static final String MJ_APIKEY_PUBLIC = "4118286da349183d3741016b1166391d";
    private static final String MJ_APIKEY_PRIVATE = "243d2294bffa386737eb41bf96968c79";

    private static final String FROM_EMAIL = "server@synqafrica.co.ke";
    private static final String FROM_NAME = "Synq-Africa";

    private volatile MailjetClient client;


    public void sendMail(String to, String subject, String text) {
        sendMail(to, subject, text, null);
    }


    public void sendMail(String to, String subject, String textPart, String htmlPart) {

        try {

            JSONObject message = new JSONObject()
                    .put(Emailv31.Message.FROM, new JSONObject()
                            .put("Email", FROM_EMAIL)
                            .put("Name", FROM_NAME))
                    .put(Emailv31.Message.TO, buildRecipients(to))
                    .put(Emailv31.Message.SUBJECT, subject);

            if (textPart != null && !textPart.isBlank()) {
                message.put(Emailv31.Message.TEXTPART, textPart);
            }

            if (htmlPart != null && !htmlPart.isBlank()) {
                message.put(Emailv31.Message.HTMLPART, htmlPart);
            }

            MailjetRequest request = new MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, new JSONArray().put(message));

            MailjetResponse response = mailjetClient().post(request);

            log.info("Response data---> :{}", response.getData());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void sendBrandedMail(String to, String subject, String heading, String bodyHtml) {

        String html = SynqEmailTemplate.render(heading, bodyHtml);

        sendMail(to, subject, SynqEmailTemplate.toPlainText(html), html);
    }


    private JSONArray buildRecipients(String emails) {

        List<String> recipients = Arrays.stream((emails == null ? "" : emails).split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        JSONArray toRecipients = new JSONArray();

        for (String email : recipients) {
            toRecipients.put(new JSONObject()
                    .put("Email", email)
                    .put("Name", email));
        }

        return toRecipients;
    }


    private MailjetClient mailjetClient() {

        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = new MailjetClient(MJ_APIKEY_PUBLIC, MJ_APIKEY_PRIVATE);
                }
            }
        }
        return client;
    }
}
