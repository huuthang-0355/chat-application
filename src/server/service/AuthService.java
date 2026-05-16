package server.service;

import server.db.UserDAO;

public class AuthService {

    private UserDAO userDAO = new UserDAO();

    public String register(String username, String password) {
        if (username == null || username.isEmpty())
            return "Username cannot be empty";

        if (password.length() < 4)
            return "Password too short";

        Integer existingId = userDAO.findUserByUsername(username);
        if (existingId != -1)
            return "Username already taken";

        String passwordHash = PasswordUtils.hashPassword(password);
        boolean success = userDAO.createUser(username, passwordHash);

        if (success)
            return "OK";
        else
            return "Registration failed";
    }

    public String login(String username, String password) {

        if (userDAO.findUserByUsername(username) == -1)
            return "Username not found";

        String storedHash = userDAO.getPasswordHash(username);
        if (storedHash == null)
            return "Username not found";

        String inputHash = PasswordUtils.hashPassword(password);

        if (inputHash.equals(storedHash))
            return "OK";

        return "Wrong password";
    }
}
