package org.example.sdpclient.service;

import jakarta.transaction.Transactional;
import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicineService {
    private final MedicineRepository repo;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    @Value("${notification.low-stock-threshold}")
    private int lowStockThreshold;

    public MedicineService(MedicineRepository repo, ActivityLogService activityLogService,
                           NotificationService notificationService) {
        this.repo = repo;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
    }

    public List<Medicine> getAll() {
        return repo.findAll(Sort.by("medicineId"));
    }

    public boolean exists(MedicineType id) {
        return repo.existsById(id);
    }

    @Transactional
    public void updateQuantity(MedicineType id, int quantity, Long adminId, String adminUsername) {
        Medicine medicine = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found: " + id));
        int oldQuantity = medicine.getQuantity();
        medicine.setQuantity(quantity);
        repo.save(medicine);

        // Log the activity
        activityLogService.logMedicineStockChange(
                medicine.getMedicineName(),
                oldQuantity,
                quantity,
                adminId,
                adminUsername
        );

        if (quantity < lowStockThreshold) {
            notificationService.notifyRootAdmins(
                    "Low stock alert: " + medicine.getMedicineName() + " has " + quantity + " units remaining."
            );
        }
    }


    @Transactional
    public void reduceQuantityByName(String medicineName, int quantityToReduce) {
        Medicine medicine = repo.findByMedicineName(medicineName)
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found: " + medicineName));

        if (quantityToReduce <= 0) {
            throw new IllegalArgumentException("Quantity must be > 0");
        }

        int current = medicine.getQuantity();
        if (current < quantityToReduce) {
            throw new IllegalArgumentException("Not enough stock. Current=" + current);
        }

        int newQuantity = current - quantityToReduce;
        medicine.setQuantity(newQuantity);
        repo.save(medicine);

        if (newQuantity < lowStockThreshold) {
            notificationService.notifyRootAdmins(
                    "Low stock alert: " + medicine.getMedicineName() + " has " + newQuantity + " units remaining."
            );
        }
    }

    public int checkLowStock() {
        List<Medicine> lowStock = repo.findAll().stream()
                .filter(m -> m.getQuantity() < lowStockThreshold)
                .toList();

        if (!lowStock.isEmpty()) {
            StringBuilder sb = new StringBuilder("Low stock alert:");
            for (Medicine m : lowStock) {
                sb.append("\n- ").append(m.getMedicineName()).append(", ").append(m.getQuantity()).append(" unit(s) left");
            }
            notificationService.notifyRootAdmins(sb.toString());
        }

        return lowStock.size();
    }
}



