package com.spa.smart_gate_springboot.account_setup.member;

import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.utils.GlobalUtils;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.util.TextUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.webjars.NotFoundException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {
    private final MemberRepository memberRepository;
    private final GlobalUtils globalUtils;

    public StandardJsonResponse findById(UUID id) {
        StandardJsonResponse response = new StandardJsonResponse();
        ChMember mem = memberRepository.findById(id).orElseThrow(() -> new NotFoundException("Member not found with id " + id));
        response.setData("result", mem, response);
        response.setMessage("message", "Ok", response);
        return response;
    }

    public StandardJsonResponse findMembersByGroupId(@NotNull UUID grpId) {
        StandardJsonResponse response = new StandardJsonResponse();
        List<ChMember> list = getChMembersByGroupId(grpId);
        response.setData("result", list, response);
        response.setTotal(list.size());
        response.setMessage("message", "Ok", response);
        return response;
    }

    public List<ChMember> getChMembersByGroupId(UUID grpId) {
        return memberRepository.findByChGroupIdOrderByChIdDesc(grpId);
    }

    public List<ChMember> getChMembersByAccountId(UUID accId) {
        return memberRepository.findByChAccIdOrderByChIdDesc(accId);
    }

    public StandardJsonResponse saveMember(ChMember chMember, User user) {
        StandardJsonResponse response = new StandardJsonResponse();

        String chCellphone = chMember.getChTelephone();

        if (chCellphone.length() == 10) {
            chCellphone = chCellphone.substring(1, 10);
            chCellphone = "254" + chCellphone;
        }
        chMember.setChTelephone(chCellphone);
        chMember.setChAccId(user.getUsrAccId());
        chMember.setChMemberCreatedBy(user.getUsrId());
        chMember.setChMemberCreatedDateTime(LocalDateTime.now());
        response.setData("result", memberRepository.saveAndFlush(chMember), response);
        response.setMessage("message", "Ok", response);
        return response;
    }

    public StandardJsonResponse deleteById(UUID id) {
        StandardJsonResponse response = new StandardJsonResponse();
        if (memberRepository.existsById(id)) {
            memberRepository.deleteById(id);
            response.setMessage("message", "member deleted successfully", response);
        } else {
            response.setMessage("message", "Failed !!! Coukd not find that member", response);
            response.setSuccess(false);
            response.setStatus(HttpStatus.SC_NOT_FOUND);
        }

        return response;
    }


    public StandardJsonResponse processExcelFile(MultipartFile file, User user, @NotNull UUID grpid) {
        StandardJsonResponse response = new StandardJsonResponse();
        List<ChMember> chMembers = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) { // Skip header row
                    continue;
                }


                for (int i = 0; i < 10; i++) {
                    log.info("row number  {} index  : {}   with value : {}", row.getRowNum(), i, globalUtils.getCellValueAsString(row.getCell(i)));
                }


                ChMember chMember = new ChMember();
                chMember.setChGroupId(grpid);
                chMember.setChAccId(user.getUsrAccId());
                chMember.setChMemberCreatedBy(user.getUsrId());
                chMember.setChMemberCreatedDateTime(LocalDateTime.now());
                chMember.setChFirstName(globalUtils.getCellValueAsString(row.getCell(0)));
                chMember.setChOtherName(globalUtils.getCellValueAsString(row.getCell(1)));
                chMember.setChGenderCode(globalUtils.getCellValueAsString(row.getCell(2)));
                String cellValue = globalUtils.getCellValueAsString(row.getCell(3));
//                if (!TextUtils.isEmpty(cellValue)) {
//                    if(cellValue.contains("/")){
//                        cellValue = cellValue.replaceAll("/", "-");
//                    }
//                    chMember.setChDob(LocalDate.parse(cellValue));
//                }


                if (!TextUtils.isEmpty(cellValue)) {
                    // Replace "/" with "-"
                    cellValue = cellValue.replaceAll("/", "-");

                    // Define a date formatter that matches the expected format
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                    try {
                        chMember.setChDob(LocalDate.parse(cellValue, formatter));
                    } catch (DateTimeParseException e) {
                        // Handle parsing errors if necessary
                        System.err.println("Invalid date format: " + cellValue);
                    }
                }

                chMember.setChNationalId(globalUtils.getCellValueAsString(row.getCell(4)));
                chMember.setChTelephone(globalUtils.getCellValueAsString(row.getCell(5)));
                chMember.setChOption1(globalUtils.getCellValueAsString(row.getCell(6)));
                chMember.setChOption2(globalUtils.getCellValueAsString(row.getCell(7)));
                chMember.setChOption3(globalUtils.getCellValueAsString(row.getCell(8)));
                chMember.setChOption4(globalUtils.getCellValueAsString(row.getCell(9)));

                globalUtils.printToJson(chMember);

                chMembers.add(chMember);
            }
            memberRepository.saveAllAndFlush(chMembers);
            response.setData("result", chMembers, response);
            response.setMessage("message", "Ok", response);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getLocalizedMessage().contains("OOXML")) {
                response.setMessage("message", "Failed !!!.  Use excell format", response);
            } else {
                response.setMessage("message", e.getLocalizedMessage(), response);
            }

            response.setSuccess(false);
            response.setStatus(500);
            return response;
        }

    }


    public byte[] createTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Members");


            // Create header row and style it
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
//            headerStyle.setLocked(true);

            String[] headers = {"First Name", "Other Names", "Gender Code", "Date of Birth", "National ID", "Telephone", "Option 1", "Option 2", "Option 3", "Option 4"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Adding a sample row of data
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("John");
            sampleRow.createCell(1).setCellValue("Doe");
            sampleRow.createCell(2).setCellValue("Male");
            sampleRow.createCell(3).setCellValue(LocalDate.now().toString());
            sampleRow.createCell(4).setCellValue("123456789");
            sampleRow.createCell(5).setCellValue("254712345678");
            sampleRow.createCell(6).setCellValue("");
            sampleRow.createCell(7).setCellValue("");
            sampleRow.createCell(8).setCellValue("");
            sampleRow.createCell(9).setCellValue("");


            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error creating template : {}", e.getLocalizedMessage() , e);
            return null;
        }
    }

    public StandardJsonResponse getAllMembersPerAccount(UUID usrAccId) {
        StandardJsonResponse response = new StandardJsonResponse();
        List<ChMember> list = getChMembersByAccountId(usrAccId);
        response.setData("result", list, response);
        response.setTotal(list.size());
        response.setMessage("message", "Ok", response);
        return response;
    }

    public void deleteByGroupidId(UUID id) {
        try {
            var groupMembers = getChMembersByGroupId(id);
            memberRepository.deleteAllInBatch(groupMembers);
        } catch (Exception e) {
            log.error("Error deleting group : {}", e.getMessage());
        }
    }
}


