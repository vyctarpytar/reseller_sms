package com.spa.smart_gate_springboot.user;

import com.spa.smart_gate_springboot.utils.AppTime;

import com.spa.smart_gate_springboot.account_setup.reseller.Reseller;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerRepo;
import com.spa.smart_gate_springboot.config.JwtService;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.mailjet.JavaEmailService;
import com.spa.smart_gate_springboot.messaging.send_message.SystemSmsService;
import com.spa.smart_gate_springboot.user.token.Token;
import com.spa.smart_gate_springboot.user.token.TokenRepository;
import com.spa.smart_gate_springboot.utils.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.util.TextUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {


    private final UserRepository userRepository;
    private final SystemSmsService systemSmsService;
    private final TokenRepository tokenRepository;
    private final JavaEmailService javaEmailService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ResellerRepo resellerRepo;
    private final GlobalUtils gu;



    @Value("${server.my-server-url}")
    private String coreGatewayUrl;

    public User save(@Valid User usr) {
        return userRepository.save(usr);
    }

    public User getCurrentUser(HttpServletRequest request) {
        String tokenFromRequest = JwtService.getTokenFromRequest(request);
        Token token = tokenRepository.findByTokenAndRevokedAndExpired(tokenFromRequest, false, false).orElseThrow(() -> new GlobalExceptionHandler.ResourceNotFoundException("Token invalid"));
        return findById(token.getUsrId());
    }

    public User findById(UUID id) {
        return userRepository.findByUsrId(id).orElseThrow(() -> new IllegalStateException("User not found with id : " + id));
    }

    public void createDefaultAdminUser(User usr) {
        coreGatewayUrl = getResellerDomain(usr.getUsrResellerId(), coreGatewayUrl);
        UniqueCodeGenerator ug = new UniqueCodeGenerator();
        String xPlainCode = ug.getUniqueCode();
        String xMessage = "Hello " + usr.getFirstname() + "," + "\nLog in to : " + coreGatewayUrl + " with username: " + usr.getEmail() + " and your start password is : " + xPlainCode;
        log.error(xMessage);
        sendMailPassword(usr.getEmail(), xMessage);
        sendPhonePassword(usr.getPhoneNumber(), xMessage);
        usr.setPassword(xPlainCode);
        usr.setUsrStatus(UsrStatus.ACTIVE);
        usr.setCreatedDate(AppTime.now());
        usr.setPassword(passwordEncoder.encode(xPlainCode));
        save(usr);
    }

    private void sendPhonePassword(String xMsisdn, String xMessage) {
        systemSmsService.sendSms(xMsisdn, xMessage);
    }

    private void sendMailPassword(String xemail, String xMessage) {
        if (TextUtils.isEmpty(xemail) || TextUtils.isEmpty(xMessage)) return;
        String xSubject = " EMAIL OTP";
        javaEmailService.sendMail(xemail, xSubject, xMessage);
    }

    public StandardJsonResponse getAllUsers(User user, UserDto filterDto) {
        StandardJsonResponse resp = new StandardJsonResponse();
        if (filterDto.getLimit() == 0) filterDto.setLimit(10);
        filterDto.setSortColumn("created_date");
        Pageable pageable = PageRequest.of(filterDto.getStart(), filterDto.getLimit(), Sort.by(filterDto.getSortColumn()).descending());

        if (!TextUtils.isEmpty(filterDto.getPhoneNumber()))
            filterDto.setPhoneNumber("%" + filterDto.getPhoneNumber() + "%");
        if (!TextUtils.isEmpty(filterDto.getFirstname())) filterDto.setFirstname("%" + filterDto.getFirstname() + "%");
        if (!TextUtils.isEmpty(filterDto.getLastname())) filterDto.setLastname("%" + filterDto.getLastname() + "%");
        if (!TextUtils.isEmpty(filterDto.getEmail())) filterDto.setEmail("%" + filterDto.getEmail() + "%");
        if (!TextUtils.isEmpty(filterDto.getUsrNationalId()))
            filterDto.setUsrNationalId("%" + filterDto.getUsrNationalId() + "%");
        Layers layer = user.getLayer();
        if (layer.equals(Layers.TOP)) {
//            filterDto.setLayer("TOP");
           if(filterDto.getResellerId() != null) filterDto.setResellerId(filterDto.getResellerId());
        }
        if (layer.equals(Layers.ACCOUNT)) filterDto.setAccId(user.getUsrAccId());
        if (layer.equals(Layers.RESELLER)) filterDto.setResellerId(user.getUsrResellerId());
        String status = null;
        if (filterDto.getUsrStatus() != null) status = filterDto.getUsrStatus().name();


        Page<User> list = userRepository.getAllUsersFiltered(filterDto.getLayer(),
                filterDto.getAccId(),
                filterDto.getResellerId(),
                filterDto.getFirstname(),
                filterDto.getLastname(),
                filterDto.getEmail(),
                status,
                filterDto.getUsrNationalId(),
                filterDto.getPhoneNumber(),
                pageable);
        resp.setMessage("message", "ok", resp);
        resp.setData("result", list.getContent(), resp);
        resp.setTotal((int) list.getTotalElements());
        return resp;
    }

    /** The tenant a user belongs to: its layer plus the reseller/account it is pinned to. */
    public record UserScope(Layers layer, UUID resellerId, UUID accId) {}

    /**
     * Creates or edits a user. {@code scope} is where the caller is working (resolved from the
     * drill-down params in UserController); a new user is stamped with it. An existing user keeps
     * its own tenancy, and may only be edited from a scope that contains it.
     */
    public StandardJsonResponse saveUser(UserDto userDto, User authed, UserScope scope) {
        StandardJsonResponse resp = new StandardJsonResponse();
        try {
            boolean isNew = userDto.getUsrId() == null;
            User user = isNew ? new User() : findById(userDto.getUsrId());

            if (!isNew && !isWithinScope(user, scope)) {
                resp.setSuccess(false);
                resp.setMessage("message", "You can't edit a user outside the reseller/account you're working in", resp);
                return resp;
            }
            Layers targetLayer = isNew ? scope.layer() : user.getLayer();
            if (userDto.getRole() == null) {
                resp.setSuccess(false);
                resp.setMessage("message", "Select a role for the user", resp);
                return resp;
            }
            if ((isNew || userDto.getRole() != user.getRole())
                    && targetLayer != null && !rolesFor(targetLayer).contains(userDto.getRole())) {
                resp.setSuccess(false);
                resp.setMessage("message", "Role " + userDto.getRole() + " is not available for " + targetLayer + " users", resp);
                return resp;
            }

            BeanUtils.copyProperties(userDto, user, gu.getNullPropertyNames(userDto));
            user.setCreatedBy(authed.getUsrId());
            user.setCreatedDate(AppTime.now());
            if (userDto.getUsrStatus() != null)
                user.setUsrStatus(userDto.getUsrStatus());
            else {
                user.setUsrStatus(UsrStatus.ACTIVE);
            }

            if(user.getUsrStatus() != UsrStatus.ACTIVE) revokeAllUserTokens(user);
            if (isNew) {
                user.setLayer(scope.layer());
                user.setUsrAccId(scope.accId());
                user.setUsrResellerId(scope.resellerId());
            }
            user.setRole(userDto.getRole());
            Set<Permission> permissions = userDto.getRole().getPermissions();
            user.getPermissions().clear();
            user.getPermissions().addAll(permissions);
            if (isNew) {
                createDefaultAdminUser(user);
                resp.setMessage("message", "user saved Successfully", resp);
            } else {
                user.setUsrId(userDto.getUsrId());
                save(user);
                resp.setMessage("message", "user Updated Successfully", resp);
            }
            resp.setData("result", user, resp);
            return resp;
        } catch (Exception e) {
            log.error("error modifying user ", e);
            resp.setSuccess(false);
            resp.setMessage("message", "Could not save the user. Please try again.", resp);
        }

        return resp;

    }

    /** True when {@code user} sits inside the tenant the caller is working in. */
    private boolean isWithinScope(User user, UserScope scope) {
        return switch (scope.layer()) {
            case TOP -> true;
            case RESELLER -> Objects.equals(user.getUsrResellerId(), scope.resellerId());
            case ACCOUNT -> Objects.equals(user.getUsrAccId(), scope.accId());
            default -> false;
        };
    }

    public StandardJsonResponse assignPermissionsToUser(UUID userId, Set<Permission> permissions) {
        StandardJsonResponse resp = new StandardJsonResponse();
        User user = findById(userId);
        user.getPermissions().clear();
        user.getPermissions().addAll(permissions);
        save(user);
        resp.setData("result", user, resp);
        resp.setMessage("message", "Permission Assigned Successfully", resp);
        return resp;
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new GlobalExceptionHandler.ResourceNotFoundException("User not found with email : " + email));
    }

    public StandardJsonResponse sendEmailOtp(User usr) {
        StandardJsonResponse response = new StandardJsonResponse();
        UniqueCodeGenerator ug = new UniqueCodeGenerator();
        String xPlainCode = ug.getUniqueCode();
        String xMessage = "Your verification code is " + xPlainCode;
        String xEmail = usr.getEmail();
        String xSubject = "EMAIL OTP";

        log.error("---{}---{}", xEmail, xMessage);

        javaEmailService.sendMail(xEmail, xSubject, xMessage);

        usr.setUsrOtpStatus("EMAIL_OTP_SENT");
        usr.setUsrEmailOtp(passwordEncoder.encode(xPlainCode));

        response.setMessage("message", "Email OTP sent successfully", response);
        response.setData("result", save(usr), response);
        return response;
    }

    public StandardJsonResponse sendPhoneOtp(User usr) {
        StandardJsonResponse response = new StandardJsonResponse();
        UniqueCodeGenerator ug = new UniqueCodeGenerator();
        String xPlainCode = ug.getUniqueCode();
        String xMessage = "Your SMS system verification code is " + xPlainCode;

        String xMsisdn = usr.getPhoneNumber();

        systemSmsService.sendSms(xMsisdn, xMessage);

        usr.setUsrOtpStatus("SMS_OTP_SENT");
        usr.setUsrPhoneOtp(passwordEncoder.encode(xPlainCode));
        response.setMessage("message", "SMS OTP sent successfully", response);
        response.setData("result", save(usr), response);
        return response;
    }

    public StandardJsonResponse validateEmailOtp(DraftSecret draftOtps) {
        StandardJsonResponse response = new StandardJsonResponse();
        User stpUser = findById(draftOtps.getUsrId());
        boolean optMatched = passwordEncoder.matches(draftOtps.getUsrSecret(), stpUser.getUsrEmailOtp());
        if (!optMatched) {
            response.setSuccess(false);
            response.setStatus(HttpStatus.SC_NOT_ACCEPTABLE);
            response.setMessage("message", "INVALID EMAIL OTP", response);
            return response;
        }
        response.setSuccess(true);
        stpUser.setUsrOtpStatus("EMAIL_OTP_VERIFIED");
        response.setMessage("message", "Email OTP matched successfully", response);
        response.setData("result", save(stpUser), response);
        return response;
    }

    public StandardJsonResponse validatePhoneOtp(DraftSecret draftOtps) {
        StandardJsonResponse response = new StandardJsonResponse();
        User stpUser = findById(draftOtps.getUsrId());
        boolean optMatched = passwordEncoder.matches(draftOtps.getUsrSecret(), stpUser.getUsrPhoneOtp());
        if (!optMatched) {
            response.setSuccess(false);
            response.setStatus(HttpStatus.SC_NOT_ACCEPTABLE);
            response.setMessage("message", "INVALID PHONE OTP", response);
            return response;
        }
        response.setSuccess(true);
        stpUser.setUsrOtpStatus("PHONE_OTP_VERIFIED");
        response.setMessage("message", "Phone OTP matched successfully", response);
        response.setData("result", save(stpUser), response);
        return response;
    }

    public StandardJsonResponse updatePasssword(DraftSecret draftSecret) {
        StandardJsonResponse response = new StandardJsonResponse();
        User user = findById(draftSecret.getUsrId());
        user.setPassword(passwordEncoder.encode(draftSecret.getUsrSecret()));
        user.setUsrChangePassword(false);
        response.setSuccess(true);
        response.setData("result", save(user), response);
        response.setMessage("message", "Paasword Updated successfully", response);
        return response;
    }

    public StandardJsonResponse newEmailOtp(UUID id, String newEmail) {

        User usr = findById(id);
        StandardJsonResponse response = new StandardJsonResponse();
        UniqueCodeGenerator ug = new UniqueCodeGenerator();
        String xPlainCode = ug.getUniqueCode();
        String xMessage = "Your E-citizen verification code is " + xPlainCode;
        String xSubject = "E-CITIZEN EMAIL OTP";
        javaEmailService.sendMail(newEmail, xSubject, xMessage);
        usr.setUsrOtpStatus("EMAIL_OTP_SENT");
        usr.setUsrEmailOtp(passwordEncoder.encode(xPlainCode));
        usr = save(usr);
        usr.setEmail(newEmail); // cache the email
        response.setMessage("message", "Email OTP sent successfully", response);
        response.setData("result", usr, response);
        return response;
    }

    public StandardJsonResponse newPhoneOtp(UUID id, String newmobile) {
        User usr = findById(id);
        StandardJsonResponse response = new StandardJsonResponse();
        UniqueCodeGenerator ug = new UniqueCodeGenerator();
        String xPlainCode = ug.getUniqueCode();
        String xMessage = "Your E-citizen verification code is " + xPlainCode;
        systemSmsService.sendSms(newmobile, xMessage);
        usr.setUsrOtpStatus("SMS_OTP_SENT");
        usr.setUsrPhoneOtp(passwordEncoder.encode(xPlainCode));
        usr = save(usr);
        usr.setPhoneNumber(newmobile);//cache the new number
        response.setMessage("message", "SMS OTP sent successfully", response);
        response.setData("result", usr, response);
        return response;
    }

    /** Roles that can be given to a user of {@code layer}. */
    public StandardJsonResponse getRoles(Layers layer) {
        StandardJsonResponse resp = new StandardJsonResponse();
        Set<Role> acceptableRoles = rolesFor(layer);
        List<RoleDto> roles = Arrays.stream(Role.values()).filter(acceptableRoles::contains).map(r -> RoleDto.builder().rlName(r.name()).build()).collect(Collectors.toList());

        resp.setTotal(roles.size());
        resp.setData("result", roles, resp);

        return resp;
    }

    private Set<Role> rolesFor(Layers layer) {
        Set<Role> roles = new HashSet<>(Set.of(Role.ADMIN, Role.VIEWER));
        if (layer == Layers.ACCOUNT) {
            roles.add(Role.CAMPAIGN_ADMIN);
        } else if (layer == Layers.RESELLER) {
            roles.addAll(Set.of(Role.SALE, Role.MANAGER, Role.ACCOUNTANT));
        } else {
            roles.add(Role.SUPER_ADMIN);
        }
        return roles;
    }

    private String getResellerDomain(UUID usrResellerId, String coreGatewayUrl) {
        if (usrResellerId == null) return coreGatewayUrl;
        Reseller rs = resellerRepo.findById(usrResellerId).orElse(null);
        if (rs == null) return coreGatewayUrl;
        String domain = rs.getRsDomain();
        return TextUtils.isEmpty(domain) ? coreGatewayUrl : domain;
    }

    public void revokeAllUserTokens(User user) {
        var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getUsrId());
        if (validUserTokens.isEmpty()) return;
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

    public List<User> findByUsrAccId(UUID accId) {
        return userRepository.findAllByUsrAccId(accId);
    }

    public void deleteUserByAccId(UUID accId, User user) {
        List<User> users = findByUsrAccId(accId);
        
        // Process user deletions in parallel using CompletableFuture
        List<CompletableFuture<Void>> deletionFutures = users.stream()
                .map(u -> CompletableFuture.runAsync(() -> {
                    try {
                        u.setUsrStatus(UsrStatus.DELETED);
                        u.setUsrDeletedBy(user.getUsrId());
                        u.setUsrDeletedDate(AppTime.now());
                        u.setUsrDeletedReason(user.getUsrDeletedReason());
                        u.setUsrDeletedByName(user.getEmail());
                        save(u);
                    } catch (Exception e) {
                        log.error("Error deleting user with ID: {}", u.getUsrId(), e);
                        throw new RuntimeException("Failed to delete user: " + u.getUsrId(), e);
                    }
                }))
                .toList();
        
        // Wait for all deletions to complete
        CompletableFuture<Void> allDeletions = CompletableFuture.allOf(
                deletionFutures.toArray(new CompletableFuture[0])
        );
        
        try {
            allDeletions.join(); // Wait for all parallel operations to complete
        } catch (Exception e) {
            log.error("Error during parallel user deletion for account: {}", accId, e);
            throw new RuntimeException("Failed to delete some users for account: " + accId, e);
        }
    }

}
