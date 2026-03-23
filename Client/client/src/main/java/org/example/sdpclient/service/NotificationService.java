package org.example.sdpclient.service;

import org.example.sdpclient.repository.AdminRepository;
import org.example.sdpclient.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final long RATE_LIMIT_MS = 60_000;

    private final PatientRepository patientRepository;
    private final AdminRepository adminRepository;
    private final SmsAsyncService smsAsyncService;

    private final ConcurrentHashMap<String, Long> rateLimitCache = new ConcurrentHashMap<>();

    public NotificationService(PatientRepository patientRepository,
                               AdminRepository adminRepository,
                               SmsAsyncService smsAsyncService) {
        this.patientRepository = patientRepository;
        this.adminRepository = adminRepository;
        this.smsAsyncService = smsAsyncService;
    }

    public void notifyPatient(Long patientId, String message) {
        patientRepository.findById(patientId).ifPresentOrElse(patient -> {
            String phone = patient.getPhone();
            if (phone == null || phone.isBlank()) {
                log.warn("Patient {} has no phone number — skipping SMS", patientId);
                return;
            }
            if (isRateLimited(phone, message)) return;
            smsAsyncService.sendSms(phone, message);
        }, () -> log.warn("Patient {} not found — skipping SMS", patientId));
    }

    public void notifyAdmin(Long adminId, String message) {
        adminRepository.findById(adminId).ifPresentOrElse(admin -> {
            String phone = admin.getPhone();
            if (phone == null || phone.isBlank()) {
                log.warn("Admin {} has no phone number — skipping SMS", adminId);
                return;
            }
            if (isRateLimited(phone, message)) return;
            smsAsyncService.sendSms(phone, message);
        }, () -> log.warn("Admin {} not found — skipping SMS", adminId));
    }

    public void notifyRootAdmins(String message) {
        adminRepository.findByRootTrue().forEach(admin -> {
            String phone = admin.getPhone();
            if (phone == null || phone.isBlank()) {
                log.warn("Root admin {} has no phone number — skipping SMS", admin.getUsername());
                return;
            }
            if (isRateLimited(phone, message)) return;
            smsAsyncService.sendSms(phone, message);
        });
    }

    private boolean isRateLimited(String phone, String message) {
        String key = phone + ":" + message;
        long now = System.currentTimeMillis();
        Long last = rateLimitCache.get(key);
        if (last != null && now - last < RATE_LIMIT_MS) {
            log.debug("Rate limited SMS to {} — duplicate within {}ms window", phone, RATE_LIMIT_MS);
            return true;
        }
        rateLimitCache.put(key, now);
        return false;
    }
}
