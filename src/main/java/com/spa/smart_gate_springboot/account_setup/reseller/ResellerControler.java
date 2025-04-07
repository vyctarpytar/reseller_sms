package com.spa.smart_gate_springboot.account_setup.reseller;


import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
@RestController
@RequestMapping("/api/v2/res")
@RequiredArgsConstructor
public class ResellerControler{
    private final ResellerService resellerService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public StandardJsonResponse saveReseller (@RequestBody Reseller reseller, HttpServletRequest request){
       var user  =  userService.getCurrentUser(request);
       reseller.setCreatedDate( LocalDateTime.now());
       reseller.setRsCreatedBy(user.getUsrId());
       return resellerService.saveReseller(reseller);
    }

    @GetMapping("/{rsId}")
    public StandardJsonResponse getResellers (@PathVariable UUID rsId, HttpServletRequest request){
        var user  =  userService.getCurrentUser(request);
      return  resellerService.getResellerById(rsId,user.getLayer().name());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public StandardJsonResponse getAllResellers ( HttpServletRequest request){
        var user  =  userService.getCurrentUser(request);
        return  resellerService.getAllReseller(user);
    }

    @GetMapping("/balance")
    public StandardJsonResponse getResellersBalance ( HttpServletRequest request){
        var user  =  userService.getCurrentUser(request);
        return  resellerService.getResellersBalance(user.getUsrResellerId());
    }


    @GetMapping("/top-balance")
    public StandardJsonResponse getTopLevelBalance ( HttpServletRequest request){
        var user  =  userService.getCurrentUser(request);
        if(!user.getLayer().equals(Layers.TOP)){
            throw new RuntimeException("User not allowed to access Top - level Resources");
        }
        return  resellerService.getTopLevelBalance();
    }
}
