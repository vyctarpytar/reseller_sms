package com.spa.smart_gate_springboot.messaging.templates;

import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.account_setup.reseller.Reseller;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerService;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import lombok.RequiredArgsConstructor;
import org.apache.http.util.TextUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TemplateService {
    private final TemplateRepository templateRepository;
    private final AccountService accountService;
    private final ResellerService resellerService;

    public StandardJsonResponse getMsgTemplate(TempFilterDto filterDto) {
        if (filterDto.getLimit() == 0) filterDto.setLimit(50);
        filterDto.setSortColumn("tmp_created_on");
        Pageable pageable = PageRequest.of(filterDto.getStart(), filterDto.getLimit(), Sort.by(filterDto.getSortColumn()).descending());

        if (!TextUtils.isEmpty(filterDto.getTmpMessage())) {
            filterDto.setTmpMessage("%" + filterDto.getTmpMessage() + "%");
        }
        StandardJsonResponse resp = new StandardJsonResponse();
        Page<Template> pagedData = templateRepository.findFilteredMessageTemplate(filterDto.getTmpAccId(), filterDto.getTmpMessage(), pageable);
        HashMap<String, Object> map = new HashMap<>();
        map.put("result", pagedData.getContent());
        resp.setData(map);

        resp.setTotal((int) pagedData.getTotalElements());
        return resp;
    }

    public StandardJsonResponse saveMsgTemplate(TempFilterDto tempFilterDto, User user) {
        Account acc = accountService.findByAccId(user.getUsrAccId());
        Reseller reseller = resellerService.findById(user.getUsrResellerId());
        Template template = Template.builder()
                .tmpName(tempFilterDto.getTmpName())
                .tmpMessage(tempFilterDto.getTmpMessage()).tmpAccById(user.getUsrAccId()).tmpAccName(acc.getAccName()).tmpCreatedOn(LocalDateTime.now())
                .tmpResellerName(reseller.getRsCompanyName()).tmpResellerById(user.getUsrResellerId()).build();
        StandardJsonResponse resp = new StandardJsonResponse();
        resp.setData("result", templateRepository.saveAndFlush(template), resp);
        resp.setMessage("message", "Template Created Successfully", resp);
        resp.setTotal(1);
        return resp;
    }

    public StandardJsonResponse updateMsgTemplate(UUID tmpId, TempFilterDto tempFilterDto, User user) {

        Template template = findById(tmpId);
        template.setTmpMessage(tempFilterDto.getTmpMessage());
        template.setTmpName(tempFilterDto.getTmpName());
        StandardJsonResponse resp = new StandardJsonResponse();
        resp.setData("result", templateRepository.saveAndFlush(template), resp);
        resp.setMessage("message", "Template Updated Successfully", resp);
        resp.setTotal(1);
        return resp;
    }

    public Template findById(UUID tmpId) {
        return templateRepository.findById(tmpId).orElseThrow(() -> new RuntimeException("Template not found with id: " + tmpId));
    }
}
