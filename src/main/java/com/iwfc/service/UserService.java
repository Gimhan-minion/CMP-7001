package com.iwfc.service;

import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.model.Role;
import com.iwfc.model.User;
import com.iwfc.pattern.NotificationCenter;
import com.iwfc.pattern.UserFactory;
import com.iwfc.repository.Repository;

import java.util.List;

public class UserService {

    private final Repository<User, String> userRepo;
    private final NotificationCenter notifications;

    public UserService(Repository<User, String> userRepo) {
        this.userRepo = userRepo;
        this.notifications = NotificationCenter.getInstance();
    }

    public User register(Role role, String id, String name, String email, String extra) {
        return register(UserFactory.create(role, id, name, email, extra));
    }

    public User register(User user) {
        userRepo.add(user);
        notifications.subscribe(user.getId(), user);
        notifications.subscribe(topicFor(user.getRole()), user);
        return user;
    }

    public User login(String id) {
        User user = userRepo.getById(id == null ? "" : id.trim().toUpperCase());
        if (!user.isActive()) {
            throw new UnauthorizedAccessException("Account " + user.getId() + " is deactivated");
        }
        return user;
    }

    public User getById(String id) {
        return userRepo.getById(id.trim().toUpperCase());
    }

    public List<User> getAll() {
        return userRepo.findAll();
    }

    public List<User> getByRole(Role role) {
        return userRepo.findBy(u -> u.getRole() == role);
    }

    public void deactivate(String id) {
        User user = getById(id);
        user.setActive(false);
        notifications.unsubscribe(topicFor(user.getRole()), user);
    }

    public void reactivate(String id) {
        User user = getById(id);
        user.setActive(true);
        notifications.subscribe(topicFor(user.getRole()), user);
    }

    private String topicFor(Role role) {
        switch (role) {
            case ADMINISTRATOR:
                return NotificationCenter.ADMINS;
            case INSTRUCTOR:
                return NotificationCenter.INSTRUCTORS;
            default:
                return NotificationCenter.MEMBERS;
        }
    }
}
