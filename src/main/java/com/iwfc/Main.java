package com.iwfc;

import com.iwfc.model.Equipment;
import com.iwfc.model.FitnessSession;
import com.iwfc.model.MaintenanceRequest;
import com.iwfc.model.User;
import com.iwfc.pattern.FitnessCenterFacade;
import com.iwfc.pattern.HoursBasedStrategy;
import com.iwfc.repository.InMemoryRepository;
import com.iwfc.repository.Repository;
import com.iwfc.service.EquipmentService;
import com.iwfc.service.MaintenanceService;
import com.iwfc.service.SchedulingService;
import com.iwfc.service.UserService;
import com.iwfc.ui.ConsoleMenu;
import com.iwfc.ui.DataSeeder;

public class Main {

    public static void main(String[] args) {
        Repository<User, String> users = new InMemoryRepository<>("User");
        Repository<Equipment, String> equipment = new InMemoryRepository<>("Equipment");
        Repository<FitnessSession, String> sessions = new InMemoryRepository<>("Session");
        Repository<MaintenanceRequest, String> requests = new InMemoryRepository<>("Maintenance request");

        UserService userService = new UserService(users);
        EquipmentService equipmentService = new EquipmentService(equipment, new HoursBasedStrategy());
        SchedulingService schedulingService = new SchedulingService(sessions, equipment);
        MaintenanceService maintenanceService = new MaintenanceService(requests, equipmentService);

        new DataSeeder(userService, equipmentService, schedulingService, maintenanceService).seed();

        FitnessCenterFacade facade = new FitnessCenterFacade(userService, equipmentService, schedulingService, maintenanceService);
        new ConsoleMenu(facade).start();
    }
}
