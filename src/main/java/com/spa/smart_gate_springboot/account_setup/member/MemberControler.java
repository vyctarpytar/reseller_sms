package com.spa.smart_gate_springboot.account_setup.member;

import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2/member")
@RequiredArgsConstructor
public class MemberControler {

    private final UserService userService;
    private final MemberService memberService;


    @GetMapping("/{grpId}")
    public StandardJsonResponse findMembersByGroupId(@PathVariable UUID grpId) {
        return memberService.findMembersByGroupId(grpId);

    }

    @GetMapping("/per-account")
    public StandardJsonResponse getAllMembersPerAccount( HttpServletRequest request) {
        User user = userService.getCurrentUser(request);
        return memberService.getAllMembersPerAccount(user.getUsrAccId());

    }


    @PostMapping
    public StandardJsonResponse saveMember(@RequestBody ChMember chMember, HttpServletRequest request) {
        User user = userService.getCurrentUser(request);
        return memberService.saveMember(chMember, user);
    }

    @GetMapping("/id/{id}")
    public StandardJsonResponse singleMember(@PathVariable UUID id) {
        return memberService.findById(id);
    }

    @DeleteMapping("/{id}")
    public StandardJsonResponse deleteGroup(@PathVariable UUID id) {
        return memberService.deleteById(id);
    }

    @PostMapping("/upload")
    public StandardJsonResponse uploadFile(@RequestParam("file") MultipartFile file, @RequestParam  @NotNull  UUID grpId, HttpServletRequest request) {
        User user = userService.getCurrentUser(request);
        return memberService.processExcelFile(file, user, grpId);
    }


    @GetMapping("/download-template")
    public ResponseEntity<byte[]> downloadDummyExcel() {
        byte[] excelBytes = memberService.createTemplate();
        if (excelBytes == null) {
            return ResponseEntity.status(500).body(null);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sample_members.xlsx");
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

}
