package com.securestaff.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public final class Database {
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = Database.class.getResourceAsStream("/db.properties")) {
            if (input == null) {
                throw new IllegalStateException("Fichier db.properties introuvable dans src/main/resources.");
            }
            PROPERTIES.load(input);
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                PROPERTIES.getProperty("db.url"),
                PROPERTIES.getProperty("db.user"),
                PROPERTIES.getProperty("db.password")
        );
    }

    public static void initializeSchema() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            addColumn(statement, "ALTER TABLE agents ADD COLUMN date_naissance DATE NULL");
            addColumn(statement, "ALTER TABLE agents ADD COLUMN sexe VARCHAR(20) NULL");
            addColumn(statement, "ALTER TABLE agents ADD COLUMN document_path VARCHAR(500) NULL");
            addColumn(statement, "ALTER TABLE agents ADD COLUMN type_contrat VARCHAR(40) NULL");

            addColumn(statement, "ALTER TABLE sites ADD COLUMN agents_jour INT NOT NULL DEFAULT 1");
            addColumn(statement, "ALTER TABLE sites ADD COLUMN agents_nuit INT NOT NULL DEFAULT 0");
            addColumn(statement, "ALTER TABLE sites ADD COLUMN date_besoin DATE NULL");

            addColumn(statement, "ALTER TABLE presences ADD UNIQUE KEY unique_planning_presence (planning_id)");

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS app_meta (" +
                            "meta_key VARCHAR(80) PRIMARY KEY, " +
                            "meta_value VARCHAR(120) NOT NULL" +
                            ")"
            );

            // Important :
            // On ne met plus de données en dur ici.
            // L'application utilise uniquement les données déjà présentes dans MySQL.

        } catch (SQLException exception) {
            throw new RuntimeException("Initialisation base impossible : " + exception.getMessage(), exception);
        }
    }

    private static void addColumn(Statement statement, String sql) {
        try {
            statement.executeUpdate(sql);
        } catch (SQLException ignored) {
            // La colonne ou la clé existe déjà.
        }
    }
}