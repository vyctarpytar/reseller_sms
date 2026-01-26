package com.spa.smart_gate_springboot.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class GlobalUtils {

    private final ObjectMapper objectMapper;

    public static String CheckNullValues(Object obj) {
        if (obj != null) {
            String myString = obj.toString();
            if (myString.trim().isEmpty()) {
                return null;
            }
            if (myString.trim().equalsIgnoreCase("null")) {
                return null;
            }

            return myString;
        }
        return null;
    }


    public static String formatPhoneNumber(String phone) {
        if (phone.length() == 10 && phone.startsWith("0")) {
            return phone.replaceFirst("0", "254");
        } else if (phone.length() == 9 && phone.startsWith("7")) {
            return phone.replaceFirst("7", "2547");
        } else if (phone.length() == 9 && phone.startsWith("1")) {
            return phone.replaceFirst("1", "2541");
        }
        return phone;
    }

    public String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();
        Set<String> emptyNames = new HashSet<String>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) emptyNames.add(pd.getName());
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

    public void printToJson(Object obj) throws JsonProcessingException {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        String jsonString = objectMapper.writeValueAsString(obj);

        log.info("see 1  >> " + jsonString);
    }

    public void printToJson(Object obj, String type) {
        try {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            String jsonString = objectMapper.writeValueAsString(obj);

            if (type.equals("success")) {
                log.info("see  >> {} " , jsonString);
            } else if (type.equalsIgnoreCase("error")) {
                log.error("error  >>  {}" , jsonString);
            }
        } catch (Exception e) {
            log.error("warn  >>  {}" , e.getMessage());
        }
    }

    public String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf(cell.getNumericCellValue());
        } else {
            return null;
        }
    }



    public String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        DataFormatter formatter = new DataFormatter();
        FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return formatter.formatCellValue(cell);
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case FORMULA:
                return formatter.formatCellValue(cell, evaluator);
            case BLANK:
                return "";
            default:
                return "";
        }
    }


    public String getCellValueAsString1(Cell cell) {
        if (cell == null) return null;

        if (cell.getCellType() == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.format("%.0f", cell.getNumericCellValue());
        } else {
            return cell.getStringCellValue();
        }
    }

    public BigDecimal getDivide(BigDecimal value1, BigDecimal value2) {
        MathContext mathContext = new MathContext(10, RoundingMode.HALF_UP);
        BigDecimal divide = value1.divide(value2, mathContext);
        divide = divide.setScale(2, RoundingMode.HALF_UP);
        if (divide.compareTo(BigDecimal.ZERO) < 0) {
            divide = BigDecimal.ONE;
        }
        return divide;
    }

    public String convertToJson(Object obj) throws JsonProcessingException {
        return objectMapper.writeValueAsString(obj);
    }
}


