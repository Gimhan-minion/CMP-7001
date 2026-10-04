package com.iwfc.ui;

import com.iwfc.exception.IwfcException;
import com.iwfc.model.Equipment;
import com.iwfc.model.EquipmentStatus;
import com.iwfc.model.FitnessSession;
import com.iwfc.model.MaintenanceRequest;
import com.iwfc.model.Role;
import com.iwfc.model.SessionType;
import com.iwfc.model.Urgency;
import com.iwfc.model.User;
import com.iwfc.pattern.EarlyWarningStrategy;
import com.iwfc.pattern.FitnessCenterFacade;
import com.iwfc.pattern.HoursBasedStrategy;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Scanner;

public class ConsoleMenu {

    private final FitnessCenterFacade facade;
    private final Scanner scanner;

    public ConsoleMenu(FitnessCenterFacade facade) {
        this.facade = facade;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("==============================================");
        System.out.println("  Intelligent Wellness and Fitness Center");
        System.out.println("==============================================");
        while (true) {
            System.out.println();
            System.out.println("Demo accounts: A001 (admin), I001 / I002 (instructors), M001 / M002 / M003 (members)");
            String id = read("Enter user ID to log in (or 'exit'): ");
            if (id.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye!");
                return;
            }
            try {
                User user = facade.login(id);
                System.out.println("Welcome, " + user.getName() + "!");
                runMenu(user);
            } catch (IwfcException e) {
                printError(e);
            }
        }
    }

    private void runMenu(User user) {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("---- " + user.getMenuTitle() + " ----  (" + user.getInbox().size() + " notifications)");
            List<String> options = optionsFor(user.getRole());
            for (int i = 0; i < options.size(); i++) {
                System.out.printf("%2d. %s%n", i + 1, options.get(i));
            }
            System.out.println(" 0. Logout");
            int choice = readInt("Choose: ");
            if (choice == 0) {
                facade.logout();
                running = false;
                continue;
            }
            if (choice < 0 || choice > options.size()) {
                System.out.println("Invalid option.");
                continue;
            }
            try {
                handle(user.getRole(), choice);
            } catch (IwfcException e) {
                printError(e);
            } catch (IllegalArgumentException | IllegalStateException | DateTimeException e) {
                System.out.println("Invalid input: " + e.getMessage());
            }
        }
    }

    private List<String> optionsFor(Role role) {
        switch (role) {
            case ADMINISTRATOR:
                return List.of("View equipment", "Add equipment", "Edit equipment", "Change equipment status",
                        "Deactivate / reactivate equipment", "View maintenance log", "Assign maintenance request",
                        "Complete maintenance request", "Preventative maintenance alerts", "Change alert strategy",
                        "View all sessions", "Schedule session", "View user accounts", "Register user",
                        "Deactivate user", "Notifications");
            case INSTRUCTOR:
                return List.of("View all sessions", "My sessions", "Schedule session", "Schedule weekly recurring session",
                        "Cancel a session", "View equipment", "Log equipment usage", "Report a fault",
                        "My fault reports", "Notifications", "View maintenance log");
            default:
                return List.of("View available sessions", "Book a session", "Book all weeks of a weekly class",
                        "My bookings", "Cancel a booking", "Notifications", "View maintenance log");
        }
    }

    private void handle(Role role, int choice) {
        switch (role) {
            case ADMINISTRATOR:
                handleAdmin(choice);
                break;
            case INSTRUCTOR:
                handleInstructor(choice);
                break;
            default:
                handleMember(choice);
        }
    }

    private void handleAdmin(int choice) {
        switch (choice) {
            case 1: printList(facade.listEquipment(), "No equipment registered."); break;
            case 2: addEquipment(); break;
            case 3: editEquipment(); break;
            case 4: changeStatus(); break;
            case 5: toggleEquipment(); break;
            case 6: printList(facade.getMaintenanceLog(), "Maintenance log is empty."); break;
            case 7: assignRequest(); break;
            case 8: completeRequest(); break;
            case 9: showAlerts(); break;
            case 10: changeStrategy(); break;
            case 11: printList(facade.getAllSessions(), "No sessions scheduled."); break;
            case 12: scheduleSession(false); break;
            case 13: printList(facade.listUsers(), "No users."); break;
            case 14: registerUser(); break;
            case 15: facade.deactivateUser(read("User ID: ")); System.out.println("User deactivated."); break;
            case 16: showNotifications(); break;
            default: System.out.println("Invalid option.");
        }
    }

    private void handleInstructor(int choice) {
        switch (choice) {
            case 1: printList(facade.getAllSessions(), "No sessions scheduled."); break;
            case 2: printList(facade.getMySessions(), "You have no sessions."); break;
            case 3: scheduleSession(false); break;
            case 4: scheduleSession(true); break;
            case 5: facade.cancelSession(read("Session ID: ")); System.out.println("Session cancelled."); break;
            case 6: printList(facade.listEquipment(), "No equipment registered."); break;
            case 7: logUsage(); break;
            case 8: reportFault(); break;
            case 9: printList(facade.getMyReports(), "You have not reported any faults."); break;
            case 10: showNotifications(); break;
            case 11: printList(facade.getMaintenanceLog(), "Maintenance log is empty."); break;
            default: System.out.println("Invalid option.");
        }
    }

    private void handleMember(int choice) {
        switch (choice) {
            case 1: printList(facade.getAvailableSessions(), "No sessions available right now."); break;
            case 2:
                printList(facade.getAvailableSessions(), "No sessions available right now.");
                FitnessSession booked = facade.bookSession(read("Session ID to book: "));
                System.out.println("Booked " + booked.getTitle() + ". See you there!");
                break;
            case 3:
                printList(facade.getAvailableSessions(), "No sessions available right now.");
                List<FitnessSession> series = facade.bookSeries(read("Session ID of the first week: "));
                System.out.println("Booked " + series.size() + " weeks:");
                printList(series, "");
                break;
            case 4: printList(facade.getMyBookings(), "You have no bookings."); break;
            case 5: facade.cancelBooking(read("Session ID to cancel: ")); System.out.println("Booking cancelled."); break;
            case 6: showNotifications(); break;
            case 7: printList(facade.getMaintenanceLog(), "Maintenance log is empty."); break;
            default: System.out.println("Invalid option.");
        }
    }

    private void addEquipment() {
        String id = read("Equipment ID: ");
        String name = read("Name: ");
        String type = read("Type (e.g. Treadmill, Spin Bike, Rowing Machine): ");
        String location = read("Location (e.g. Cardio Zone, Studio A): ");
        Equipment equipment = facade.addEquipment(id, name, type, location);
        System.out.println("Added " + equipment.getId() + ".");
    }

    private void editEquipment() {
        String id = read("Equipment ID: ");
        String name = read("New name (blank to keep): ");
        String location = read("New location (blank to keep): ");
        System.out.println(facade.editEquipment(id, name, location));
    }

    private void changeStatus() {
        String id = read("Equipment ID: ");
        EquipmentStatus status = readEnum(EquipmentStatus.class, "New status");
        System.out.println(facade.changeEquipmentStatus(id, status));
    }

    private void toggleEquipment() {
        String id = read("Equipment ID: ");
        String action = read("(d)eactivate or (r)eactivate? ");
        if (action.equalsIgnoreCase("r")) {
            facade.reactivateEquipment(id);
            System.out.println("Equipment reactivated.");
        } else {
            facade.deactivateEquipment(id);
            System.out.println("Equipment deactivated.");
        }
    }

    private void assignRequest() {
        printList(facade.getOpenRequests(), "No open requests.");
        String id = read("Request ID: ");
        String technician = read("Assign to (technician name): ");
        MaintenanceRequest request = facade.assignRequest(id, technician);
        System.out.println("Request " + request.getId() + " is now " + request.getStatus());
    }

    private void completeRequest() {
        printList(facade.getOpenRequests(), "No open requests.");
        MaintenanceRequest request = facade.completeRequest(read("Request ID: "));
        System.out.println("Request " + request.getId() + " is now " + request.getStatus());
    }

    private void showAlerts() {
        System.out.println("Current rule: " + facade.getThresholdStrategyName());
        printList(facade.getDueForMaintenance(), "No equipment is due for maintenance.");
    }

    private void changeStrategy() {
        System.out.println("1. Hours based (alert at full service interval)");
        System.out.println("2. Early warning (alert at 80% of service interval)");
        int option = readInt("Choose: ");
        facade.setThresholdStrategy(option == 2 ? new EarlyWarningStrategy(0.8) : new HoursBasedStrategy());
        System.out.println("Alert rule set to " + facade.getThresholdStrategyName());
    }

    private void registerUser() {
        Role role = readEnum(Role.class, "Role");
        String id = read("User ID: ");
        String name = read("Full name: ");
        String email = read("Email: ");
        String extra = null;
        if (role == Role.INSTRUCTOR) {
            extra = read("Specialisation: ");
        } else if (role == Role.MEMBER) {
            extra = read("Membership plan: ");
        }
        System.out.println("Registered " + facade.registerUser(role, id, name, email, extra).getId());
    }

    private void scheduleSession(boolean recurring) {
        FitnessSession.Builder builder = new FitnessSession.Builder()
                .title(read("Title: "))
                .type(readEnum(SessionType.class, "Session type"))
                .studio(read("Studio (e.g. Studio A): "));
        if (facade.getCurrentUser().getRole() == Role.ADMINISTRATOR) {
            builder.instructor(read("Instructor ID: ").toUpperCase());
        }
        LocalDate date = LocalDate.parse(read("Date (yyyy-mm-dd): "));
        LocalTime start = LocalTime.parse(read("Start time (HH:mm): "));
        int minutes = readInt("Duration in minutes: ");
        builder.time(date.atTime(start), date.atTime(start).plusMinutes(minutes));
        builder.capacity(readInt("Capacity: "));
        String equipment = read("Equipment IDs, comma separated (blank for none): ");
        if (!equipment.isBlank()) {
            builder.equipment(equipment.split("\\s*,\\s*"));
        }

        if (recurring) {
            int weeks = readInt("Number of weeks: ");
            List<FitnessSession> series = facade.scheduleRecurring(builder, weeks);
            System.out.println("Created " + series.size() + " weekly sessions:");
            printList(series, "");
        } else {
            System.out.println("Scheduled: " + facade.scheduleSession(builder));
        }
    }

    private void logUsage() {
        String id = read("Equipment ID: ");
        double hours = readDouble("Hours used: ");
        boolean alert = facade.logUsage(id, hours);
        System.out.println("Usage logged." + (alert ? " Service limit reached - administrators have been alerted." : ""));
    }

    private void reportFault() {
        String id = read("Equipment ID: ");
        String description = read("Describe the issue: ");
        Urgency urgency = readEnum(Urgency.class, "Urgency");
        MaintenanceRequest request = facade.reportFault(id, description, urgency);
        System.out.println("Fault logged as " + request.getId() + " (" + request.getStatus() + ")");
    }

    private void showNotifications() {
        List<String> inbox = facade.getNotifications();
        if (inbox.isEmpty()) {
            System.out.println("No notifications.");
            return;
        }
        inbox.forEach(n -> System.out.println("  * " + n));
        if (read("Clear notifications? (y/n): ").equalsIgnoreCase("y")) {
            facade.clearNotifications();
        }
    }

    private <T> void printList(List<T> items, String emptyMessage) {
        if (items.isEmpty()) {
            System.out.println(emptyMessage);
            return;
        }
        items.forEach(item -> System.out.println("  " + item));
    }

    private <E extends Enum<E>> E readEnum(Class<E> type, String label) {
        E[] values = type.getEnumConstants();
        while (true) {
            for (int i = 0; i < values.length; i++) {
                System.out.printf("   %d. %s%n", i + 1, values[i]);
            }
            int choice = readInt(label + ": ");
            if (choice >= 1 && choice <= values.length) {
                return values[choice - 1];
            }
            System.out.println("Pick a number from the list.");
        }
    }

    private String read(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            System.exit(0);
        }
        return scanner.nextLine().trim();
    }

    private int readInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(read(prompt));
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private double readDouble(String prompt) {
        while (true) {
            try {
                return Double.parseDouble(read(prompt));
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    private void printError(IwfcException e) {
        System.out.println("[" + e.getClass().getSimpleName() + "] " + e.getMessage());
    }
}
