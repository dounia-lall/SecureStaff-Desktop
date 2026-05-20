package com.securestaff.dao;

import com.securestaff.db.Database;
import com.securestaff.model.Incident;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class IncidentDao {
    public List<Incident> findAll() {
        List<Incident> incidents = new ArrayList<>();
        String sql = """
                SELECT i.*, CONCAT(a.prenom, ' ', a.nom) AS agent_name, s.nom AS site_name
                FROM incidents i
                JOIN agents a ON a.id = i.agent_id
                JOIN sites s ON s.id = i.site_id
                ORDER BY i.date_incident DESC
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                incidents.add(new Incident(
                        rs.getInt("id"), rs.getInt("site_id"), rs.getString("site_name"), rs.getInt("agent_id"), rs.getString("agent_name"),
                        rs.getString("description"), rs.getString("gravite"),
                        rs.getTimestamp("date_incident").toLocalDateTime()
                ));
            }
            return incidents;
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void save(int siteId, int agentId, String description, String gravite) {
        String sql = "INSERT INTO incidents (site_id, agent_id, description, gravite) VALUES (?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, siteId);
            statement.setInt(2, agentId);
            statement.setString(3, description);
            statement.setString(4, gravite);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void restore(Incident incident) {
        String sql = "INSERT INTO incidents (id, site_id, agent_id, description, gravite, date_incident) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, incident.getId());
            statement.setInt(2, incident.getSiteId());
            statement.setInt(3, incident.getAgentId());
            statement.setString(4, incident.getDescription());
            statement.setString(5, incident.getGravite());
            statement.setTimestamp(6, Timestamp.valueOf(incident.getDateIncident()));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void delete(int id) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM incidents WHERE id=?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void update(int id, int siteId, int agentId, String description, String gravite) {
        String sql = "UPDATE incidents SET site_id=?, agent_id=?, description=?, gravite=? WHERE id=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, siteId);
            statement.setInt(2, agentId);
            statement.setString(3, description);
            statement.setString(4, gravite);
            statement.setInt(5, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }
}
