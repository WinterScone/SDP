package org.example.sdpclient.service;

import org.example.sdpclient.entity.Admin;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.repository.AdminRepository;
import org.example.sdpclient.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminMessagingService {

    public record RecipientDto(Long id, String role, String name, boolean hasPhone) {}
    public record RecipientEntry(Long id, String role) {}
    public record SendResult(int sent, int skippedNoPhone, int failed, List<String> errors) {}

    private final SmsService smsService;
    private final AdminRepository adminRepository;
    private final PatientRepository patientRepository;
    private final ActivityLogService activityLogService;

    public AdminMessagingService(SmsService smsService, AdminRepository adminRepository,
                                  PatientRepository patientRepository, ActivityLogService activityLogService) {
        this.smsService = smsService;
        this.adminRepository = adminRepository;
        this.patientRepository = patientRepository;
        this.activityLogService = activityLogService;
    }

    public List<RecipientDto> getAllRecipients(Long senderAdminId) {
        List<RecipientDto> recipients = new ArrayList<>();

        for (Admin admin : adminRepository.findAll()) {
            if (admin.getId().equals(senderAdminId)) continue;
            boolean hasPhone = admin.getPhone() != null && !admin.getPhone().isBlank();
            String name = admin.getFirstName() + " " + admin.getLastName();
            recipients.add(new RecipientDto(admin.getId(), "ADMIN", name, hasPhone));
        }

        for (Patient patient : patientRepository.findAll()) {
            boolean hasPhone = patient.getPhone() != null && !patient.getPhone().isBlank();
            String name = patient.getFirstName() + " " + patient.getLastName();
            recipients.add(new RecipientDto(patient.getId(), "PATIENT", name, hasPhone));
        }

        return recipients;
    }

    public SendResult sendMessage(Long senderAdminId, String senderUsername,
                                   List<RecipientEntry> recipients, String messageBody) {
        String fullBody = "[SDP - Root Admin " + senderUsername + "] " + messageBody;

        int sent = 0;
        int skippedNoPhone = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (RecipientEntry entry : recipients) {
            String phone = getPhone(entry);
            String name = getName(entry);

            if (phone == null || phone.isBlank()) {
                skippedNoPhone++;
                continue;
            }

            String error = smsService.sendSms(phone, fullBody);
            if (error == null) {
                sent++;
            } else {
                failed++;
                errors.add(name + ": " + error);
            }
        }

        activityLogService.logMessageSent(senderAdminId, senderUsername, recipients.size());

        return new SendResult(sent, skippedNoPhone, failed, errors);
    }

    private String getPhone(RecipientEntry entry) {
        if ("ADMIN".equals(entry.role())) {
            return adminRepository.findById(entry.id()).map(Admin::getPhone).orElse(null);
        } else {
            return patientRepository.findById(entry.id()).map(Patient::getPhone).orElse(null);
        }
    }

    private String getName(RecipientEntry entry) {
        if ("ADMIN".equals(entry.role())) {
            return adminRepository.findById(entry.id())
                    .map(a -> a.getFirstName() + " " + a.getLastName())
                    .orElse("Unknown Admin");
        } else {
            return patientRepository.findById(entry.id())
                    .map(p -> p.getFirstName() + " " + p.getLastName())
                    .orElse("Unknown Patient");
        }
    }
}
