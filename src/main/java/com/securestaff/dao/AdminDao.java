package com.securestaff.dao;

import com.securestaff.db.Database;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminDao {

    public boolean login(String username, String password) {

        String sql = "SELECT password FROM admins WHERE username = ?";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    String hashedPassword = resultSet.getString("password");
                    return BCrypt.checkpw(password, hashedPassword);
                }
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Connexion admin impossible : " + exception.getMessage(), exception);
        }

        return false;
    }
}