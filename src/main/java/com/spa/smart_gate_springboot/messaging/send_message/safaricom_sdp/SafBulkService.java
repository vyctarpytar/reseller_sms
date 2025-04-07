package com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp;


import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.account_setup.shortsetup.MsgShortcodeSetup;
import com.spa.smart_gate_springboot.account_setup.shortsetup.MsgShortcodeSetupService;
import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArc;
import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArcRepository;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.SafAuthService;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.SafaricomInterface;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.SafaricomProperties;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.dto.BulkResponse;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.dto.ResponseModel;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.models.SafBulkDataSet;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.models.SendBulkSafReq;
import com.spa.smart_gate_springboot.utils.AppUtils;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.apache.http.util.TextUtils;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Service
@Slf4j
@RequiredArgsConstructor
public class SafBulkService {

    private final SafaricomInterface safaricomInterface;
    private final SafAuthService safAuthService;
    private final SafaricomProperties safaricomProperties;
    private final MsgMessageQueueArcRepository msgMessageQueueArcRepository;

    private final Retrofit safComRetrofit;
    private final MsgShortcodeSetupService msgShortcodeSetupService;
    private final AccountService accountService;


    public void sendArcSms(MsgMessageQueueArc msg) throws Exception {


        accountService.handleUpdateOfAccountBalance(msg.getMsgCostId(), msg.getMsgAccId(), msg.getMsgResellerId());

        SendBulkSafReq sendBulkSafReq = new SendBulkSafReq();

        List<SafBulkDataSet> safBulkDataSetList = new ArrayList<>();
        SafBulkDataSet safBulkDataSet = getSafBulkDataSet(msg.getMsgMessage(), msg.getMsgSubMobileNo(), msg.getMsgCode(), msg.getMsgSenderIdName().trim(), msg.getMsgAccId());
        safBulkDataSetList.add(safBulkDataSet);
        sendBulkSafReq.setDataSet(safBulkDataSetList);

        sendBulkToSaf(sendBulkSafReq);
    }


    public void sendBulkToSaf(SendBulkSafReq sendBulkSafReq) throws Exception {

        sendBulkSafReq.setTimeStamp(String.valueOf(System.currentTimeMillis()));
//        sendBulkSafReq.getDataSet().get(0).setOa("Hakibet-OTP");

        ResponseModel body;


        String accessToken = safAuthService.getAccessToken();
        if (TextUtils.isEmpty(accessToken)) {
            // get new Token
            safAuthService.getTokenFromSafaricom();

            // requeue message to RMQ
            throw new RuntimeException("XXXX --- > Failed to get access token");
        }


        Call<BulkResponse> call = safaricomInterface.sendBulkSms(APPLICATION_JSON_VALUE, "XMLHttpRequest", "Bearer " + accessToken, sendBulkSafReq);

        Response<BulkResponse> res = call.execute();


        if (res.isSuccessful()) {

            assert res.body() != null;

            if (res.body().getStatusCode().equalsIgnoreCase(AppUtils.BULK_SMS_SEND_SUCCESS_STATUS_CODE)) {

                body = ResponseModel.builder().status("00").message("success").data(res.body()).success(true).batchSize(sendBulkSafReq.getDataSet().size()).build();
                updateMessageStatus(true, sendBulkSafReq.getDataSet(), res.code(), res.body().getStatusCode());
            } else {
                log.info("[SEND BULK RESPONSE MEANS WAS SENT FAILED WITH STATUS CODE {} ]", res.body().getStatusCode());

                body = ResponseModel.builder().status("00").message(res.body().getStatus()).data(res.body()).build();
                updateMessageStatus(false, sendBulkSafReq.getDataSet(), res.code(), res.body().getStatusCode());
            }


        } else {

            BulkResponse ob = parseError(res.errorBody());
            body = ResponseModel.builder().status("01").message("Failed to send message").data(ob).build();
            log.error("[SAF SEND BULK ERROR RES {} ]", ob);
            updateMessageStatus(false, sendBulkSafReq.getDataSet(), res.code(), ob.getMessage());
        }

//        return body;
    }

    private void updateMessageStatus(boolean success, List<SafBulkDataSet> dataSet, int code, String safResponse) {
        List<String> msgCodesList = dataSet.stream().map(SafBulkDataSet::getUniqueId).toList();
        msgMessageQueueArcRepository.updateInitialReceiveNote(success ? "SAF_ACCEPTED_SENT" : "SAF_RECEIVE_FAILED", code, msgCodesList, safResponse);
    }


    private BulkResponse parseError(ResponseBody errorBody) {
        Converter<ResponseBody, BulkResponse> converter = safComRetrofit.responseBodyConverter(BulkResponse.class, new java.lang.annotation.Annotation[0]);

        BulkResponse errorResponse = null;
        try {
            if (errorBody != null) {
                errorResponse = converter.convert(errorBody);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return errorResponse;
    }

    public SafBulkDataSet getSafBulkDataSet(String message, String msisdn, String messageId, String senderId, @NotNull(message = "field cannot be null") UUID msgAccId) {

        String packageId = safaricomProperties.getSafPromotionalPackageId();
        MsgShortcodeSetup shortcodeSetup = msgShortcodeSetupService.findByShCodeAndShAccId(senderId,msgAccId);

        if (TextUtils.isEmpty(shortcodeSetup.getShSenderType())) throw new RuntimeException("Sender Id Type not mapped!!");
        if(shortcodeSetup.getShSenderType().equalsIgnoreCase("TRANSACTION")) packageId = safaricomProperties.getSafTransactionalPackageId();

        SafBulkDataSet safBulkDataSet = new SafBulkDataSet();
        safBulkDataSet.setMessage(message);
        safBulkDataSet.setMsisdn(msisdn);
        safBulkDataSet.setChannel(AppUtils.CHANNEL_SMS);
        safBulkDataSet.setOa(senderId);
        safBulkDataSet.setPackageId(packageId);
        safBulkDataSet.setUniqueId(messageId);
        safBulkDataSet.setUserName(safaricomProperties.getSafUserName());
        safBulkDataSet.setActionResponseURL(safaricomProperties.getSafResponseUrl());
        return safBulkDataSet;
    }

}
