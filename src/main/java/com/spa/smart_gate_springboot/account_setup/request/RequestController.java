package com.spa.smart_gate_springboot.account_setup.request;

import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v2/req")
public class RequestController {
    private final RequestService requestService;
    private final UserService userService;

    @PostMapping("/sender-Id")
    public StandardJsonResponse saveSenderIdRequests(@RequestBody SenderIdReqDto senderIdReqDto, HttpServletRequest request) {
        var auth = userService.getCurrentUser(request);
        return requestService.saveSenderIdRequests(senderIdReqDto, auth);
    }

    @PostMapping("/ussd-code")
    public StandardJsonResponse saveUssdRequests(@RequestBody SenderIdReqDto senderIdReqDto, HttpServletRequest request) {
        var auth = userService.getCurrentUser(request);
        return requestService.saveUssdRequests(senderIdReqDto, auth);
    }

    @PostMapping("/short-code")
    public StandardJsonResponse saveShortCodeRequests(@RequestBody SenderIdReqDto senderIdReqDto, HttpServletRequest request) {
        var auth = userService.getCurrentUser(request);
        return requestService.saveShortCodeRequests(senderIdReqDto, auth);
    }

    @PostMapping("/file-upload")
    public ResponseEntity<StandardJsonResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        StandardJsonResponse response = new StandardJsonResponse();
        try {
            String filename = requestService.saveFile(file);
            response.setMessage("message", "File uploaded successfully", response);
            response.setData("result", filename, response);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            response.setSuccess(false);
            response.setMessage("message", "File upload failed: " + e.getLocalizedMessage(), response);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping()
    public StandardJsonResponse filterRequestByStatus(@RequestParam("reqStatus") String reqStatus, HttpServletRequest request) {
        var auth = userService.getCurrentUser(request);
        return requestService.filterRequestByStatus(reqStatus.toUpperCase(), auth);
    }

    @PostMapping("/trigget-in-progress/{regId}")
    public StandardJsonResponse triggerInProgress(@PathVariable UUID regId, HttpServletRequest request) {
        var auth = userService.getCurrentUser(request);
        return requestService.triggerInProgress(regId, auth);
    }

    @GetMapping("/{reqId}")
    public StandardJsonResponse fetchRequestById(@PathVariable UUID reqId, HttpServletRequest request) {
//        var auth = userService.getCurrentUser(request);
        return requestService.fetchRequestById(reqId);
    }


    @PostMapping("invalidate/{reqId}")
    public StandardJsonResponse invalidateRequest(@PathVariable UUID reqId, HttpServletRequest request) {
        var auth = userService.getCurrentUser(request);
        return requestService.invalidateRequest(reqId,auth);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            return ResponseEntity.status(404).body(null);
        }

        FileSystemResource resource = new FileSystemResource(file);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

        return ResponseEntity.ok().headers(headers).contentLength(file.length()).body(resource);
    }

}


