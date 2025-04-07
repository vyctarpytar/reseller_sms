package com.spa.smart_gate_springboot.ndovuPay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spa.smart_gate_springboot.account_setup.reseller.Reseller;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerRepo;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.ndovuPay.withdraw.*;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.GlobalUtils;
import com.spa.smart_gate_springboot.utils.SmartGate;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import com.spa.smart_gate_springboot.utils.UniqueCodeGenerator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.util.TextUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class NdovupayService {
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ResellerNdovuPayRepository ndovuPayRepo;
    private final RestTemplate restTemplate;
    private final ResellerRepo resellerService;
    private final GlobalUtils gu;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final WithDrawRequestRepository withDrawRequestRepository;
    String ndovuIp = "102.217.125.47";
    private final String CREATE_ORG_URL = "http://" + ndovuIp + "/api/create_organization.action"; // Replace with your actual URL
    private final String ORG_BALANCE_url = "http://" + ndovuIp + "/api/fetchOrganization.action?orgCode="; // Replace with your actual URL
    private final String WITHDRAW_COLLECTION_WALLET = "http://" + ndovuIp + "/api/saveWithdraw.action"; // Replace with your actual URL
    private final String NDOVUPAY_TAARIFF = "http://" + ndovuIp + "/api/fetchTariffs.action"; // Replace with your actual URL
    private final String GET_TOKEN = "http://" + ndovuIp + "/api/authenitcate.action?user=smartgate&password=Qwerty@1&key=dsgfmdknugrjrbrtb&secret=kjg23r6348rgf3784fb4774b97vn5v95v";

    public String getToken() {
        try {
            StandardJsonResponse response = restTemplate.getForObject(GET_TOKEN, StandardJsonResponse.class);

            if (response == null) {
                throw new RuntimeException("Invalid response from authentication service");
            }
            return response.getData().get("token").toString();
        } catch (Exception e) {
            // Log the exception and rethrow or handle it as needed
            throw new RuntimeException(e);
        }
    }

    public void createNdovuPayOrganisation(Reseller rs) {

        // Set up the headers with the Authorization token
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", getToken()); // Assuming a Bearer token. Adjust if needed.
        headers.set("Content-Type", "application/json");
        // Create an HttpEntity with the headers

        log.info("reseller name {}", rs.getRsCompanyName());
        UniqueCodeGenerator ug = new UniqueCodeGenerator();
        String xPlainCode = ug.getUniqueCode();

        Organisation organisation = Organisation.builder().orgCounty("KENYA").orgName(rs.getRsCompanyName()).orgEmail(rs.getRsEmail()).orgMobile(rs.getRsPhoneNumber()).orgKraPin(rs.getRsKraPin()).orgLocation(rs.getRsContactPerson()).orgTown(rs.getRaCity()).usrLastName(rs.getRsContactPerson()).usrFirstName(rs.getRsCompanyName()).usrEmail(rs.getRsEmail()).usrMobileNumber(rs.getRsPhoneNumber()).usrEncryptedPassword(passwordEncoder.encode(xPlainCode)).orgKraPin(xPlainCode).build();
        HttpEntity<Organisation> entity = new HttpEntity<>(organisation, headers);

        // Make the POST request with the token in the headers
        ResponseEntity<StandardJsonResponse> responseEntity = null;
        try {
            responseEntity = restTemplate.exchange(CREATE_ORG_URL, HttpMethod.POST, entity, StandardJsonResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Get the Organisation object from the response
        var org = responseEntity.getBody();
        if (org != null && org.isSuccess()) {
            gu.printToJson(org, "error");
            ResellerNdovuPay ndovuPay = ResellerNdovuPay.builder().ndBranch(org.getData().get("branch").toString()).ndResellerId(rs.getRsId())
                    .ndDisbursement(org.getData().get("disbursement").toString()).ndOrganization((Integer) org.getData().get("organization"))
                    .ndCollection(org.getData().get("collection").toString())
                    .ndLayers(Layers.RESELLER)
                    .build();
            ndovuPayRepo.saveAndFlush(ndovuPay);
            rs.setRsHasNdovuPayAccount(true);
            resellerService.saveAndFlush(rs);
        } else {
            gu.printToJson(org, "error");
            System.out.println("Failed to create organisation");
            rs.setRsHasNdovuPayAccount(false);
            resellerService.saveAndFlush(rs);
        }


    }

    public StandardJsonResponse getOrgBalances(UUID rsId) throws JsonProcessingException {
        StandardJsonResponse response = new StandardJsonResponse();
        ResellerNdovuPay rsNdovuPay = ndovuPayRepo.findByNdResellerId(rsId).orElseThrow(() -> new RuntimeException("Reseller Not found with id:" + rsId));
        var resp = getResponse(rsNdovuPay);
        if (resp != null && resp.isSuccess()) {

            var wallets = resp.getData().get("wallets");

            List<Wallet> walletList = objectMapper.readValue(objectMapper.writeValueAsString(wallets), new TypeReference<>() {
            });


            // todo delete teemp
            rsNdovuPay.setNdCollection(walletList.get(0).getWalCode());
            ndovuPayRepo.saveAndFlush(rsNdovuPay);

            response.setData("result", walletList, response);
            return response;
        }
        response.setMessage("message", "Wallet Data not Available", response);
        response.setStatus(404);
        response.setSuccess(false);
        return response;

    }

    private @Nullable StandardJsonResponse getResponse(ResellerNdovuPay rsNdovuPay) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", getToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        int ndOrganization = rsNdovuPay.getNdOrganization();

        ResponseEntity<StandardJsonResponse> responseEntity = restTemplate.exchange(ORG_BALANCE_url + ndOrganization + "&walType=COLLECTION", HttpMethod.POST, entity, StandardJsonResponse.class);
        log.error(" ndovupay org is ({}) \n -- {}", ndOrganization, responseEntity.getBody());
        return responseEntity.getBody();
    }

//    @Scheduled(fixedRate = 5000)
//    public void getBal(){
//        try {
//            var aa = getOrgBalances(UUID.fromString("d9221e5a-f7d2-4be9-b29f-0fa85dbe30e4"));
//          gu.printToJson(aa, "error");
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//    }


    public String getCollectWalletCode(UUID rsId) throws IOException {
        ResellerNdovuPay rsNdovuPay = ndovuPayRepo.findByNdResellerId(rsId).orElseThrow(() -> new RuntimeException("Reseller Not found with id:" + rsId));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", getToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = ORG_BALANCE_url + rsNdovuPay.getNdOrganization() + "&walType=COLLECTION";
        ResponseEntity<StandardJsonResponse> responseEntity = restTemplate.exchange(url, HttpMethod.POST, entity, StandardJsonResponse.class);
        var resp = responseEntity.getBody();
        if (resp != null && resp.isSuccess()) {
            var wallets = resp.getData().get("wallets");
            List<Wallet> walletList = objectMapper.readValue(objectMapper.writeValueAsString(wallets), new TypeReference<>() {
            });
            gu.printToJson(walletList, "error");

            return walletList.get(0).getWalCode();
        }
        return null;

    }


    public StandardJsonResponse initiateWithDrawOtp(User usr, WithDrawDto withDrawDto) {
        StandardJsonResponse response = new StandardJsonResponse();
        UniqueCodeGenerator ug = new UniqueCodeGenerator();
        String xPlainCode = ug.getUniqueCode();
        String xMessage = "Use code  " + xPlainCode + " to send ksh:" + withDrawDto.getWithDrawAmount() + " to phone: " + withDrawDto.getWithDrawPhoneNumber();
        log.info("opt message : {}", xMessage);
        String xMsisdn = usr.getPhoneNumber();

        new Thread(() -> {
            SmartGate smartGate = new SmartGate();
            smartGate.sendSMS(xMsisdn, xMessage);
        }).start();
        WithDrawRequest req = logWithDrawRequest(usr, withDrawDto);
        withDrawDto.setWithDrawLogId(req.getWithDrawId());
        usr.setUsrOtpStatus("WITHDRAW_OTP_SENT");
        usr.setUsrPhoneWithdrawOtp(passwordEncoder.encode(xPlainCode));
        response.setMessage("message", "Withdraw OTP sent to " + xMsisdn + " successfully", response);
        response.setData("result", withDrawDto, response);
        userService.save(usr);

        return response;
    }


    public StandardJsonResponse finalizeWithdrawOtp(User user, WithDrawDto withDrawDto) {
        StandardJsonResponse response = new StandardJsonResponse();
        boolean optMatched = passwordEncoder.matches(withDrawDto.getWithDrawCode(), user.getUsrPhoneWithdrawOtp());
        if (!optMatched) {
            response.setSuccess(false);
            response.setStatus(HttpStatus.SC_NOT_ACCEPTABLE);
            response.setMessage("message", "INVALID PHONE OTP", response);
            return response;
        }
        return finalizeWithDraw(user, withDrawDto);
    }





    public StandardJsonResponse finalizeWithDraw(User usr, WithDrawDto withDrawDto) {
        ResellerNdovuPay rsNdovuPay = ndovuPayRepo.findByNdResellerId(usr.getUsrResellerId()).orElseThrow(() -> new RuntimeException("Reseller Not found with id:" + usr.getUsrResellerId()));
        if (withDrawDto.getWithDrawLogId() == null) throw new RuntimeException("Request Id is not available");
        WithDrawRequest req = findByPendingValidationId(withDrawDto.getWithDrawLogId());


        if (!req.getWithDrawStatus().equals(WithDrawStatus.PENDING_OTP_VALIDATION)) {
            StandardJsonResponse response = new StandardJsonResponse();
            response.setSuccess(false);
            response.setStatus(HttpStatus.SC_NOT_ACCEPTABLE);
            response.setMessage("message", "Please Retry Later.. Such a request Exists", response);
            log.error("XXX: WITHDRAW ERROR -> Please wait.. Such a request Exists ({})", withDrawDto.getWithDrawLogId());
            return response;
        }


        req.setWithDrawStatus(WithDrawStatus.ONGOING);
        withDrawRequestRepository.saveAndFlush(req);
        withDrawDto.setWithDrawLogId(req.getWithDrawId());
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", getToken());
        WithDrawNdovuPayLoad payLoad = WithDrawNdovuPayLoad.builder().apwAmount(withDrawDto.getWithDrawAmount()).apwNumber(withDrawDto.getWithDrawPhoneNumber())
                .apwOrgCode(rsNdovuPay.getNdOrganization()).apwWalCode(rsNdovuPay.getNdCollection()).apwDesc("SMS CREDIT WITHDRAW")
                .apwType("MPESA")
                .apwReceiverName(usr.getEmail()).build();
        gu.printToJson(WITHDRAW_COLLECTION_WALLET, "error");
        gu.printToJson(payLoad, "error");
        HttpEntity<WithDrawNdovuPayLoad> entity = new HttpEntity<>(payLoad, headers);
        ResponseEntity<StandardJsonResponse> responseEntity = null;
        try {
            responseEntity = restTemplate.exchange(WITHDRAW_COLLECTION_WALLET, HttpMethod.POST, entity, StandardJsonResponse.class);
        } catch (Exception e) {
            req.setWithDrawStatus(WithDrawStatus.FAILED);
            req.setWithDrawErrorDesc(e.getLocalizedMessage());
            withDrawRequestRepository.saveAndFlush(req);
            throw new RuntimeException("Error Ocurred at NDOVU-PAY " + e.getLocalizedMessage());
        }
        var resp = responseEntity.getBody();
        log.error("resp--  \n{}", resp);
        gu.printToJson(resp, "error");

        req.setWithDrawStatus(WithDrawStatus.SUCCESS);
        withDrawRequestRepository.saveAndFlush(req);

        resp.setData("res", withDrawDto, resp);
        return resp;

    }

    private WithDrawRequest findByPendingValidationId(UUID withDrawLogId) {
        return withDrawRequestRepository.findById(withDrawLogId).orElseThrow(() -> new RuntimeException("Uknown Source Request"));
    }

    private WithDrawRequest logWithDrawRequest(User user, WithDrawDto withDrawDto) {
        WithDrawRequest req = WithDrawRequest.builder().withDrawAmount(withDrawDto.getWithDrawAmount()).withDrawStatus(WithDrawStatus.PENDING_OTP_VALIDATION).withDrawPhoneNumber(withDrawDto.getWithDrawPhoneNumber()).withDrawCode(user.getUsrPhoneWithdrawOtp()).withDrawCreatedDate(LocalDateTime.now()).withDrawCreatedByEmail(user.getEmail()).withDrawCreatedById(user.getUsrId()).withDrawResellerId(user.getUsrResellerId()).build();
        return withDrawRequestRepository.saveAndFlush(req);
    }


    public StandardJsonResponse getNdovuPayRates() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", getToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);
//        int ndOrganization = rsNdovuPay.getNdOrganization();

        ResponseEntity<StandardJsonResponse> responseEntity = restTemplate.exchange(NDOVUPAY_TAARIFF, HttpMethod.POST, entity, StandardJsonResponse.class);
        return responseEntity.getBody();
    }

    public StandardJsonResponse fetchWithdrawRequest(User user, WithDrawFilter filterDto) {
        if (filterDto.getLimit() == 0) filterDto.setLimit(10);
        filterDto.setSortColumn("with_draw_created_date");
        Pageable pageable = PageRequest.of(filterDto.getStart(), filterDto.getLimit(), Sort.by(filterDto.getSortColumn()).descending());
        if (!TextUtils.isEmpty(filterDto.getWithDrawSubmobileNo())) {
            filterDto.setWithDrawSubmobileNo("%" + filterDto.getWithDrawSubmobileNo() + "%");
        }
        if (!TextUtils.isEmpty(filterDto.getWithDrawCreatedByName())) {
            filterDto.setWithDrawCreatedByName("%" + filterDto.getWithDrawCreatedByName() + "%");
        }
        if (!TextUtils.isEmpty(filterDto.getWithDrawStatus())) {
            filterDto.setWithDrawStatus("%" + filterDto.getWithDrawStatus() + "%");
        }

        Date msgDate = filterDto.getWithDrawCreatedDate();
        Date msgDateFrom = filterDto.getWithDrawDateFrom();
        Date msgDateTo = filterDto.getWithDrawDateTo();
        if (msgDate == null) msgDate = new Date();

        if (msgDateTo == null) msgDateTo = new Date();
        if (msgDateFrom != null) msgDate = null;


        Page<WithDrawRequest> pagedData = withDrawRequestRepository.getFilteredWithDrawRequest(filterDto.getWithDrawResellerId(),
                filterDto.getWithDrawCreatedByName(), msgDate, msgDateFrom, msgDateTo, filterDto.getWithDrawStatus(),filterDto.getWithDrawSubmobileNo(), pageable);
        StandardJsonResponse response = new StandardJsonResponse();
        response.setData("result", pagedData.getContent(), response);
        response.setTotal((int) pagedData.getTotalElements());
        return response;
    }

    public StandardJsonResponse getDistinctWithDrawStatuses() {
        StandardJsonResponse resp = new StandardJsonResponse();
        List<String> statusDto = Arrays.stream(WithDrawStatus.values()).map(Enum::name).collect(Collectors.toList());
        resp.setTotal(statusDto.size());
        resp.setData("result", statusDto, resp);
        return resp;
    }

    //    @PostConstruct
//    private Reseller wiserRs() {
//        Reseller rs = Reseller.builder()
//                .rsSmsUnitPrice(BigDecimal.valueOf(0.1))
//                .rsCompanyName("Weiser")
//                .rsEmail("Weiser")
//                .rsPhoneNumber("Weiser")
//                .rsContactPerson("Weiser")
//                .raCity("Weiser")
//                .rsLogo("Weiser")
//                .raState("Weiser")
//                .raPostalCode("Weiser")
//                .raCountry("Weiser")
//                .raWebsite("Weiser")
//                .vatNumber("Weiser")
//                .createdDate(LocalDateTime.now())
//                .isActive(true)
//                .rsBusinessType(BusinessType.COMPANY)
//
//                .rsMsgBal(BigDecimal.valueOf(100000000))
//                .rsStatus("Weiser")
//                .rsKraPin("Weiser")
//                .rsHasNdovuPayAccount(false)
//                .build();
//        return resellerService.saveAndFlush(rs);
//    }
//    @PostConstruct
    public void init1() {
        List<Reseller> reseller = resellerService.findByRsHasNdovuPayAccountIsFalseOrRsHasNdovuPayAccountIsNull();
        for (Reseller r : reseller) {
            log.info("reseller " + r.getRsId());
            try {
                createNdovuPayOrganisation(r);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }




/*    @PostConstruct
    public void init() {
        // try Withdraw

        WithDrawDto withDrawDto = WithDrawDto.builder()
                .withDrawAmount(new BigDecimal("100"))
                .withDrawCode("ZOLI6P")
                .withDrawLogId(UUID.fromString("25b7c708-5c63-4cb5-8fc2-1193164579bf"))
                .withDrawPhoneNumber("254716177880")
                .build();

        User usr =  userService.findByEmail("accounts@spa-limited.com");
        finalizeWithDraw( usr,  withDrawDto);
    }

    */
}

