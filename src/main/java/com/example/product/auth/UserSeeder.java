package com.example.product.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class UserSeeder implements CommandLineRunner {
    private final UserService userService;
    private final String seedUsers;

    public UserSeeder(UserService userService,
                      @Value("${app.auth.users:}") String seedUsers) {
        this.userService = userService;
        this.seedUsers = seedUsers;
    }

    @Override
    public void run(String... args) {
        userService.seedUsers(seedUsers);
    }
}
