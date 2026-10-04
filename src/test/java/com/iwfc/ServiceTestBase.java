package com.iwfc;

import com.iwfc.model.Equipment;
import com.iwfc.model.FitnessSession;
import com.iwfc.model.MaintenanceRequest;
import com.iwfc.model.Role;
import com.iwfc.model.SessionType;
import com.iwfc.model.User;
import com.iwfc.pattern.FitnessCenterFacade;
import com.iwfc.pattern.HoursBasedStrategy;
import com.iwfc.pattern.NotificationCenter;
import com.iwfc.repository.InMemoryRepository;
import com.iwfc.service.EquipmentService;
import com.iwfc.service.MaintenanceService;
import com.iwfc.service.SchedulingService;
import com.iwfc.service.UserService;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDate;

public abstract class ServiceTestBase {

    protected UserService userService;
    protected EquipmentService equipmentService;
    protected SchedulingService schedulingService;
    protected MaintenanceService maintenanceService;
    protected FitnessCenterFacade facade;
    protected LocalDate day;

    @BeforeEach
    void setUpBase() {
        NotificationCenter.getInstance().reset();

        InMemoryRepository<Equipment, String> equipment = new InMemoryRepository<>("Equipment");
        userService = new UserService(new InMemoryRepository<User, String>("User"));
        equipmentService = new EquipmentService(equipment, new HoursBasedStrategy());
        schedulingService = new SchedulingService(new InMemoryRepository<FitnessSession, String>("Session"), equipment);
        maintenanceService = new MaintenanceService(new InMemoryRepository<MaintenanceRequest, String>("Request"), equipmentService);
        facade = new FitnessCenterFacade(userService, equipmentService, schedulingService, maintenanceService);

        userService.register(Role.ADMINISTRATOR, "A1", "Admin User", "admin@iwfc.lk", null);
        userService.register(Role.INSTRUCTOR, "I1", "Kasun Fernando", "kasun@iwfc.lk", "HIIT");
        userService.register(Role.INSTRUCTOR, "I2", "Dilini Jay", "dilini@iwfc.lk", "Yoga");
        userService.register(Role.MEMBER, "M1", "Amal Perera", "amal@gmail.com", "Premium");
        userService.register(Role.MEMBER, "M2", "Sachini W", "sachini@gmail.com", "Standard");
        userService.register(Role.MEMBER, "M3", "Ravi Kumar", "ravi@gmail.com", "Standard");

        equipmentService.addEquipment("EQ1", "Spin Bike 04", "Spin Bike", "Studio B");
        equipmentService.addEquipment("EQ2", "Spin Bike 05", "Spin Bike", "Studio B");
        equipmentService.addEquipment("EQ3", "Treadmill 01", "Treadmill", "Cardio Zone");

        day = LocalDate.now().plusDays(7);
    }

    protected FitnessSession.Builder session(String title, String studio, int startHour, int endHour) {
        return new FitnessSession.Builder()
                .title(title)
                .type(SessionType.HIIT)
                .instructor("I1")
                .studio(studio)
                .time(day.atTime(startHour, 0), day.atTime(endHour, 0))
                .capacity(10);
    }
}
