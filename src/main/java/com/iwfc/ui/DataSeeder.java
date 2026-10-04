package com.iwfc.ui;

import com.iwfc.model.FitnessSession;
import com.iwfc.model.Role;
import com.iwfc.model.SessionType;
import com.iwfc.model.Urgency;
import com.iwfc.model.User;
import com.iwfc.service.EquipmentService;
import com.iwfc.service.MaintenanceService;
import com.iwfc.service.SchedulingService;
import com.iwfc.service.UserService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

public class DataSeeder {

    private final UserService userService;
    private final EquipmentService equipmentService;
    private final SchedulingService schedulingService;
    private final MaintenanceService maintenanceService;

    public DataSeeder(UserService userService, EquipmentService equipmentService,
                      SchedulingService schedulingService, MaintenanceService maintenanceService) {
        this.userService = userService;
        this.equipmentService = equipmentService;
        this.schedulingService = schedulingService;
        this.maintenanceService = maintenanceService;
    }

    public void seed() {
        seedUsers();
        seedEquipment();
        seedSessions();
        seedMaintenance();
        userService.getAll().forEach(User::clearInbox);
    }

    private void seedUsers() {
        userService.register(Role.ADMINISTRATOR, "A001", "Nimal Silva", "nimal@iwfc.lk", null);
        userService.register(Role.INSTRUCTOR, "I001", "Kasun Fernando", "kasun@iwfc.lk", "HIIT");
        userService.register(Role.INSTRUCTOR, "I002", "Dilini Jayasinghe", "dilini@iwfc.lk", "Yoga & Pilates");
        userService.register(Role.MEMBER, "M001", "Amal Perera", "amal@gmail.com", "Premium");
        userService.register(Role.MEMBER, "M002", "Sachini Wickrama", "sachini@gmail.com", "Standard");
        userService.register(Role.MEMBER, "M003", "Ravi Kumar", "ravi@yahoo.com", "Standard");
    }

    private void seedEquipment() {
        equipmentService.addEquipment("EQ01", "Treadmill 01", "Treadmill", "Cardio Zone");
        equipmentService.addEquipment("EQ02", "Treadmill 02", "Treadmill", "Cardio Zone");
        equipmentService.addEquipment("EQ03", "Spin Bike 04", "Spin Bike", "Studio B");
        equipmentService.addEquipment("EQ04", "Spin Bike 05", "Spin Bike", "Studio B");
        equipmentService.addEquipment("EQ05", "Rowing Machine 01", "Rowing Machine", "Cardio Zone");
        equipmentService.addEquipment("EQ06", "Cross Trainer 01", "Cross Trainer", "Cardio Zone");
        equipmentService.addEquipment("EQ07", "HR Monitor Set A", "Heart Rate Monitor", "Studio A");
        equipmentService.addEquipment("EQ08", "Spin Bike 06", "Spin Bike", "Studio B");

        equipmentService.logUsage("EQ01", 320);
        equipmentService.logUsage("EQ02", 145.5);
        equipmentService.logUsage("EQ03", 210);
        equipmentService.logUsage("EQ04", 96);
        equipmentService.logUsage("EQ05", 388);
        equipmentService.logUsage("EQ08", 290);
    }

    private void seedSessions() {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

        schedulingService.createRecurring(new FitnessSession.Builder()
                .title("Monday Morning Pilates")
                .type(SessionType.PILATES)
                .instructor("I002")
                .studio("Studio A")
                .time(monday.atTime(7, 0), monday.atTime(8, 0))
                .capacity(15), 6);

        LocalDate tuesday = monday.plusDays(1);
        FitnessSession hiit = schedulingService.createSession(new FitnessSession.Builder()
                .title("HIIT Blast")
                .type(SessionType.HIIT)
                .instructor("I001")
                .studio("Studio A")
                .time(tuesday.atTime(18, 0), tuesday.atTime(19, 0))
                .capacity(12));

        LocalDate wednesday = monday.plusDays(2);
        schedulingService.createSession(new FitnessSession.Builder()
                .title("Sunrise Yoga")
                .type(SessionType.YOGA)
                .instructor("I002")
                .studio("Studio C")
                .time(wednesday.atTime(LocalTime.of(6, 30)), wednesday.atTime(7, 30))
                .capacity(20));

        LocalDate thursday = monday.plusDays(3);
        FitnessSession spin = schedulingService.createSession(new FitnessSession.Builder()
                .title("Spin Express")
                .type(SessionType.SPIN)
                .instructor("I001")
                .studio("Studio B")
                .equipment("EQ03", "EQ04")
                .time(thursday.atTime(17, 30), thursday.atTime(18, 15))
                .capacity(2));

        schedulingService.bookSession("M001", hiit.getId());
        schedulingService.bookSession("M002", hiit.getId());
        schedulingService.bookSession("M001", spin.getId());
    }

    private void seedMaintenance() {
        maintenanceService.reportFault("I001", "EQ06", "Squeaking noise from left pedal", Urgency.LOW);
        String id = maintenanceService.reportFault("I002", "EQ07", "Heart rate monitor calibration error", Urgency.MEDIUM).getId();
        maintenanceService.assign(id, "TechFit Services");
    }
}
