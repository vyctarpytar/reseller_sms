package com.spa.smart_gate_springboot.account_setup.request;

import com.spa.smart_gate_springboot.account_setup.reseller.Reseller;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerService;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.utils.SmartGate;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.apache.http.util.TextUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RequestService {

    public static String uploadDir = "/opt/images/OTP_IMAGES/FILES/";
    private final RequestRepo requestRepo;
    private final SmartGate smartGate;
    private final ResellerService resellerService;
    @Value("${server.my-server-url}")
    private String coreGatewayUrl;

    private void notifyWeiser(ReServiceType reServiceType, Reseller rs) {

        String xMessage = rs.getRsCompanyName() + " has made a service request (" + reServiceType.name() + ");\n Login to " + coreGatewayUrl + " to process the request";
        smartGate.sendSMS("254716177880", xMessage);
    }

    public StandardJsonResponse saveShortCodeRequests(SenderIdReqDto senderIdReqDto, User auth) {
        Reseller reseller = resellerService.findById(auth.getUsrResellerId());
        RequestEntity req = RequestEntity.builder().reInstType(senderIdReqDto.getInstType()).reCreatedDate(LocalDateTime.now()).reCreatedBy(auth.getUsrId()).reDesc(senderIdReqDto.getDesc()).reTelcos(senderIdReqDto.getTelcos()).reStatus(ReStatus.PENDING).reResellerId(auth.getUsrResellerId()).reServiceOwnership(ServiceOwnership.valueOf(senderIdReqDto.getServiceOwnership())).reKeyWord(senderIdReqDto.getKeyword()).reServiceType(ReServiceType.SHORTCODE).reName(resellerService.findById(auth.getUsrResellerId()).getRsCompanyName()).reKraFileName(senderIdReqDto.getReKraFileName()).reIncorporationCertFileName(senderIdReqDto.getReIncorporationCertFileName()).reAuthorizationFileName(senderIdReqDto.getReAuthorizationFileName()).build();

        save(req);


        notifyWeiser(req.getReServiceType(), reseller);


        StandardJsonResponse resp = new StandardJsonResponse();
        resp.setData("result", req, resp);
        resp.setMessage("message", "Request saved successfully", resp);
        return resp;
    }

    public StandardJsonResponse saveUssdRequests(SenderIdReqDto senderIdReqDto, User auth) {
        Reseller reseller = resellerService.findById(auth.getUsrResellerId());
        RequestEntity req = RequestEntity.builder().reInstType(senderIdReqDto.getInstType()).reCreatedDate(LocalDateTime.now()).reCreatedBy(auth.getUsrId()).reDesc(senderIdReqDto.getDesc()).reTelcos(senderIdReqDto.getTelcos()).reStatus(ReStatus.PENDING).reResellerId(auth.getUsrResellerId()).reCostCover(CostCover.valueOf(senderIdReqDto.getCostCover())).reServiceOwnership(ServiceOwnership.valueOf(senderIdReqDto.getServiceOwnership())).reServiceType(ReServiceType.USSD).reName(reseller.getRsCompanyName()).reKraFileName(senderIdReqDto.getReKraFileName()).reIncorporationCertFileName(senderIdReqDto.getReIncorporationCertFileName()).reAuthorizationFileName(senderIdReqDto.getReAuthorizationFileName()).build();

        save(req);


        notifyWeiser(req.getReServiceType(), reseller);


        StandardJsonResponse resp = new StandardJsonResponse();
        resp.setData("result", req, resp);
        resp.setMessage("message", "Request saved successfully", resp);
        return resp;
    }

    public StandardJsonResponse saveSenderIdRequests(SenderIdReqDto senderIdReqDto, User auth) {
        Reseller reseller = resellerService.findById(auth.getUsrResellerId());
        RequestEntity req = RequestEntity.builder().reInstType(senderIdReqDto.getInstType()).reCreatedDate(LocalDateTime.now()).reCreatedBy(auth.getUsrId()).reDesc(senderIdReqDto.getDesc()).reTelcos(senderIdReqDto.getTelcos()).reSenderIdType(SenderIdType.valueOf(senderIdReqDto.getSenderIdType())).reStatus(ReStatus.PENDING).reResellerId(auth.getUsrResellerId()).reName(resellerService.findById(auth.getUsrResellerId()).getRsCompanyName()).reServiceType(ReServiceType.SMS).reKraFileName(senderIdReqDto.getReKraFileName()).reIncorporationCertFileName(senderIdReqDto.getReIncorporationCertFileName()).reAuthorizationFileName(senderIdReqDto.getReAuthorizationFileName()).build();

        save(req);

        //EMAIL JOHN / VICTOR
        notifyWeiser(req.getReServiceType(), reseller);
        StandardJsonResponse resp = new StandardJsonResponse();
        resp.setData("result", req, resp);
        resp.setMessage("message", "Request saved successfully", resp);
        return resp;
    }

    public RequestEntity save(RequestEntity requesEntity) {
        return requestRepo.saveAndFlush(requesEntity);
    }


    public String saveFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file.");
        }

        // 10 MB
        long MAX_FILE_SIZE = 10 * 1024 * 1024;
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("Failed to store file. The file size exceeds the limit of 10 MB.");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String filename = timestamp + "_" + originalFilename;

        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        try (InputStream inputStream = file.getInputStream()) {
            Path filePath = uploadPath.resolve(filename);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            return "/opt/images/OTP_IMAGES/FILES/" + filename;
        } catch (IOException e) {
            throw new IOException("Failed to store file " + filename, e);
        }
    }


    public StandardJsonResponse filterRequestByStatus(String reqStatus, User auth) {
        StandardJsonResponse resp = new StandardJsonResponse();

        if (!TextUtils.isEmpty(reqStatus)) {
            if (!ReStatus.exists(reqStatus)) {
                resp.setMessage("message", "Invalid Status Provided", resp);
                resp.setSuccess(false);
                return resp;
            }
        }

        List<RequestEntity> reqList = new ArrayList<>();
        if (auth.getLayer().equals(Layers.TOP)) {
            reqList = requestRepo.findByRequestStatus(ReStatus.valueOf(reqStatus));

        }
        if (auth.getLayer().equals(Layers.RESELLER)) {
            reqList = requestRepo.findByReStatusAndReResellerId(ReStatus.valueOf(reqStatus), auth.getUsrResellerId());
        }

        resp.setData("result", reqList, resp);
        resp.setTotal(reqList.size());
        return resp;
    }

    public RequestEntity findByid(UUID reqId) {
        return requestRepo.findById(reqId).orElseThrow(() -> new RuntimeException("Request not found with id :" + reqId));
    }

    public StandardJsonResponse triggerInProgress(UUID regId, User auth) {
        StandardJsonResponse resp = new StandardJsonResponse();
        RequestEntity req = findByid(regId);
        req.setReStatus(ReStatus.PROCESSING);
        save(req);
        resp.setData("result", req, resp);
        resp.setMessage("result", "Status updated to processing", resp);
        return resp;
    }

    public StandardJsonResponse fetchRequestById(UUID reqId) {
        StandardJsonResponse resp = new StandardJsonResponse();
        RequestEntity req = findByid(reqId);
        resp.setData("result", req, resp);
        resp.setTotal(1);
        return resp;
    }

    public RequestEntity findByReSetUpId(UUID shId) {
        return requestRepo.findByReSetUpId(shId).orElse(null);
    }

    public StandardJsonResponse invalidateRequest(UUID reqId, User auth) {
        StandardJsonResponse resp = new StandardJsonResponse();
        RequestEntity req = findByid(reqId);
        if (req.getReStatus().equals(ReStatus.PENDING)) {
            req.setReStatus(ReStatus.DELETED);
            req.setReUpdatedBy(auth.getUsrId());
            req.setReUpdatedDate(LocalDateTime.now());
            resp.setData("result", save(req), resp);
            resp.setTotal(1);
            return resp;
        }
        resp.setStatus(HttpStatus.SC_NOT_ACCEPTABLE);
        resp.setSuccess(false);
        resp.setMessage("message", "Invalid Status Provided", resp);
        return resp;
    }
}
