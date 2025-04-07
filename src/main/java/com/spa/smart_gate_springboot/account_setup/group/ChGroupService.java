package com.spa.smart_gate_springboot.account_setup.group;

import com.spa.smart_gate_springboot.account_setup.member.MemberService;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChGroupService {


    private final ChGroupRepository chGroupRepository;
    private final MemberService memberService;

    public List<ChGroup> findAll() {
        return chGroupRepository.findAll();
    }

    public StandardJsonResponse findById(UUID id) {
        StandardJsonResponse response = new StandardJsonResponse();
        response.setData("result",  getChGroup(id), response);
        response.setTotal(1);
        response.setMessage("message", "Group deleted successfully", response);
        return response;
    }

    private ChGroup getChGroup(UUID id) {
     return chGroupRepository.findById(id).orElseThrow(()-> new NotFoundException("Group not found with id "+ id));
    }

    public ChGroup save(ChGroup chGroup) {
        return chGroupRepository.saveAndFlush(chGroup);
    }

    public StandardJsonResponse deleteById(UUID id) {
        StandardJsonResponse response = new StandardJsonResponse();
        if(chGroupRepository.existsById(id)){
            memberService.deleteByGroupidId(id);
            chGroupRepository.deleteById(id);
        }

        response.setMessage("message", "Group deleted successfully", response);
        return response;

    }

    public StandardJsonResponse findByAccountId(@NotNull UUID accId) {
        StandardJsonResponse response = new StandardJsonResponse();
        List<ChGroup> list = chGroupRepository.findByGroupAccId(accId);
        response.setData("result", list, response);
        response.setTotal(list.size());
        response.setMessage("message", "Ok", response);
        return response;
    }

    public StandardJsonResponse createGroup(ChGroup grp, User user) {
        StandardJsonResponse response = new StandardJsonResponse();
        grp.setGroupCreationDate(LocalDateTime.now());
        grp.setGroupCreatedBy(user.getUsrId());
        grp.setGroupCreatedByName(user.getFirstname());
        grp.setGroupAccId(user.getUsrAccId());
        response.setData("result", save(grp), response);
        response.setMessage("message", "Group created successfully", response);
        return response;
    }
}

