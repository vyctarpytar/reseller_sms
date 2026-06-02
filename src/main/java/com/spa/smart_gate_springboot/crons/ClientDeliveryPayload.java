package com.spa.smart_gate_springboot.crons;

import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArc;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Delivery report payload that is POSTed to a client's {@code callbackUrl}
 * once a sent message has a delivery report from the carrier.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDeliveryPayload {

    /** The msgExternalId the client supplied when sending the message. */
    private String messageId;

    /** Destination phone number the message was sent to. */
    private String mobileNo;

    /** Sender ID / short code the message was sent from. */
    private String senderId;

    /** The message body that was sent. */
    private String message;

    /** Carrier delivery status, e.g. "DeliveredToTerminal", "Success", "DeliveryImpossible". */
    private String status;

    /** Carrier status / error code, e.g. "200". */
    private String statusCode;

    /** Human readable status description from the carrier. */
    private String statusDescription;

    /** When the delivery report was received. */
    private LocalDateTime deliveredAt;

    /** Carrier request / reference id for the delivery. */
    private String requestId;

    public static ClientDeliveryPayload from(MsgMessageQueueArc m) {
        return ClientDeliveryPayload.builder()
                .messageId(m.getMsgExternalId())
                .mobileNo(m.getMsgSubMobileNo())
                .senderId(m.getMsgSenderIdName())
                .message(m.getMsgMessage())
                .status(m.getMsgStatus())
                .statusCode(m.getMsgErrorCode())
                .statusDescription(m.getMsgErrorDesc())
                .deliveredAt(m.getMsgDeliveredDate())
                .requestId(m.getMsgRequestId())
                .build();
    }
}
