package org.example.sdpclient.service;

import org.example.sdpclient.entity.Admin;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.repository.AdminRepository;
import org.example.sdpclient.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int RATE_LIMIT_MINUTES = 1;

    private final SmsAsyncService smsAsyncService;
    private final PatientRepository patientRepository;
    private final AdminRepository adminRepository;

    private final ConcurrentHashMap<String, LocalDateTime> rateLimitMap = new ConcurrentHashMap<>();

    public NotificationService(SmsAsyncService smsAsyncService,
                               PatientRepository patientRepository,
                               AdminRepository adminRepository) {
        this.smsAsyncService = smsAsyncService;
        this.patientRepository = patientRepository;
        this.adminRepository = adminRepository;
    }

    public void notifyPatient(Long patientId, String message) {
        Patient patient = patientRepository.findById(patientId).orElse(null);
        if (patient == null || patient.getPhone() == null || patient.getPhone().isBlank()) {
            log.debug("Cannot notify patient {} — no phone number", patientId);
            return;
        }
        if (!patient.isSmsConsent()) {
            log.debug("Patient {} has not consented to SMS", patientId);
            return;
        }
        sendWithRateLimit("patient-" + patientId, patient.getPhone(), message);
    }

    public void notifyAdmin(Long adminId, String message) {
        Admin admin = adminRepository.findById(adminId).orElse(null);
        if (admin == null || admin.getPhone() == null || admin.getPhone().isBlank()) {
            log.debug("Cannot notify admin {} — no phone number", adminId);
            return;
        }
        sendWithRateLimit("admin-" + adminId, admin.getPhone(), message);
    }

    public void notifyRootAdmins(String message) {
        List<Admin> rootAdmins = adminRepository.findByRootTrue();
        for (Admin admin : rootAdmins) {
            if (admin.getPhone() != null && !admin.getPhone().isBlank()) {
                sendWithRateLimit("admin-" + admin.getId(), admin.getPhone(), message);
            }
        }
    }

    private void sendWithRateLimit(String key, String phone, String message) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastSent = rateLimitMap.get(key);
        if (lastSent != null && lastSent.plusMinutes(RATE_LIMIT_MINUTES).isAfter(now)) {
            log.debug("Rate-limited SMS to {} (key={})", phone, key);
            return;
        }
        rateLimitMap.put(key, now);
        smsAsyncService.sendSmsAsync(phone, message);
    }
}
