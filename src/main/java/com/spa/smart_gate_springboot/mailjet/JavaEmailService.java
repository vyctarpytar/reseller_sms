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


    public  void sendMail(String to, String subject, String text) {

        try {


            MailjetClient client = new MailjetClient(MJ_APIKEY_PUBLIC, MJ_APIKEY_PRIVATE);

            List<String> recipients = Arrays.stream(to.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

            JSONArray toRecipients = new JSONArray();
            for (String email : recipients) {
                toRecipients.put(new JSONObject()
                        .put("Email", email)
                        .put("Name", email)); // you can also make name dynamic if needed
            }

            MailjetRequest request = new MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, new JSONArray()
                            .put(new JSONObject()
                                    .put(Emailv31.Message.FROM, new JSONObject()
                                            .put("Email", "server@synqafrica.co.ke")
                                            .put("Name", "Synq-Africa"))
                                    .put(Emailv31.Message.TO, toRecipients)
                                    .put(Emailv31.Message.SUBJECT, subject)
                                    .put(Emailv31.Message.TEXTPART, text)
//                                    .put(Emailv31.Message.HTMLPART, "<h3>Dear passenger 1, welcome to <a href=\"https://www.mailjet.com/\">Mailjet</a>!</h3><br />May the delivery force be with you!")
                            ));




            MailjetResponse response = client.post(request);

           log.info("Response data---> :{}",response.getData());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
