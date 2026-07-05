package com.cipherlink.service;

import com.cipherlink.dto.*;
import com.cipherlink.model.*;
import com.cipherlink.repository.*;
import com.cipherlink.security.JwtService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ValidationService {

    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) return false;
        return phoneNumber.matches("^\\+[1-9]\\d{6,14}$");
    }

    public boolean isValidBase64(String base64) {
        if (base64 == null || base64.isBlank()) return false;
        try {
            Base64.getDecoder().decode(base64);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isValidFileSize(String base64Data, long maxBytes) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64Data);
            return bytes.length <= maxBytes;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAllowedFileType(String fileName) {
        String[] allowed = {
            ".jpg", ".jpeg", ".png", ".gif", ".webp",
            ".mp4", ".mov", ".avi",
            ".mp3", ".aac", ".ogg", ".m4a",
            ".pdf", ".doc", ".docx", ".txt"
        };
        String ext = fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf(".")).toLowerCase() : "";
        return Arrays.asList(allowed).contains(ext);
    }

    public String sanitizeText(String input) {
        if (input == null || input.isBlank()) return "";
        input = input.replaceAll("<.*?>", "");
        input = input.replaceAll("(?i)javascript:", "");
        return input.trim();
    }

    public boolean isValidDisplayName(String name) {
        if (name == null || name.isBlank()) return false;
        if (name.length() < 2 || name.length() > 50) return false;
        return name.matches("^[a-zA-Z0-9\\s\\-_.]+$");
    }

    public boolean isValidGroupName(String name) {
        if (name == null || name.isBlank()) return false;
        return name.length() >= 2 && name.length() <= 100;
    }
}
