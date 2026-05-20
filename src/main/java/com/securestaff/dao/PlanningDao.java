package com.securestaff.dao;

import com.securestaff.db.Database;
import com.securestaff.model.Planning;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlanningDao {
    public List<Planning> findAll() {
        List<Planning> plannings = new ArrayList<>();
        String sql = """
                SELECT p.*, CONCAT(a.prenom, ' ', a.nom) AS agent_name, s.nom AS site_name, s.adresse AS site_adresse
                FROM plannings p
                JOIN agents a ON a.id = p.agent_id
                JOIN sites s ON s.id = p.site_id
                ORDER BY p.date_service DESC, p.heure_debut
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                plannings.add(new Planning(
                        rs.getInt("id"), rs.getInt("agent_id"), rs.getString("agent_name"),
                        rs.getInt("site_id"), rs.getString("site_name"), rs.getString("site_adresse"),
                        rs.getDate("date_service").toLocalDate(),
                        rs.getTime("heure_debut").toLocalTime(),
                        rs.getTime("heure_fin").toLocalTime(),
                        rs.getString("type_service")
                ));
            }
            return plannings;
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public List<Planning> findByAgentAndMonth(int agentId, int year, int month) {
        List<Planning> plannings = new ArrayList<>();
        String sql = """
                SELECT p.*, CONCAT(a.prenom, ' ', a.nom) AS agent_name, s.nom AS site_name, s.adresse AS site_adresse
                FROM plannings p
                JOIN agents a ON a.id = p.agent_id
                JOIN sites s ON s.id = p.site_id
                WHERE p.agent_id = ? AND YEAR(p.date_service) = ? AND MONTH(p.date_service) = ?
                ORDER BY p.date_service, p.heure_debut
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, agentId);
            statement.setInt(2, year);
            statement.setInt(3, month);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    plannings.add(new Planning(
                            rs.getInt("id"), rs.getInt("agent_id"), rs.getString("agent_name"),
                            rs.getInt("site_id"), rs.getString("site_name"), rs.getString("site_adresse"),
                            rs.getDate("date_service").toLocalDate(),
                            rs.getTime("heure_debut").toLocalTime(),
                            rs.getTime("heure_fin").toLocalTime(),
                            rs.getString("type_service")
                    ));
                }
            }
            return plannings;
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public List<Planning> findByMonth(int year, int month) {
        List<Planning> plannings = new ArrayList<>();
        String sql = """
                SELECT p.*, CONCAT(a.prenom, ' ', a.nom) AS agent_name, s.nom AS site_name, s.adresse AS site_adresse
                FROM plannings p
                JOIN agents a ON a.id = p.agent_id
                JOIN sites s ON s.id = p.site_id
                WHERE YEAR(p.date_service) = ? AND MONTH(p.date_service) = ?
                ORDER BY a.nom, a.prenom, p.date_service, p.heure_debut
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, year);
            statement.setInt(2, month);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    plannings.add(new Planning(
                            rs.getInt("id"), rs.getInt("agent_id"), rs.getString("agent_name"),
                            rs.getInt("site_id"), rs.getString("site_name"), rs.getString("site_adresse"),
                            rs.getDate("date_service").toLocalDate(),
                            rs.getTime("heure_debut").toLocalTime(),
                            rs.getTime("heure_fin").toLocalTime(),
                            rs.getString("type_service")
                    ));
                }
            }
            return plannings;
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void save(Planning planning) {
        String sql = "INSERT INTO plannings (agent_id, site_id, date_service, heure_debut, heure_fin, type_service) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, planning.getAgentId());
            statement.setInt(2, planning.getSiteId());
            statement.setDate(3, Date.valueOf(planning.getDateService()));
            statement.setTime(4, Time.valueOf(planning.getHeureDebut()));
            statement.setTime(5, Time.valueOf(planning.getHeureFin()));
            statement.setString(6, planning.getTypeService());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void restore(Planning planning) {
        String sql = "INSERT INTO plannings (id, agent_id, site_id, date_service, heure_debut, heure_fin, type_service) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, planning.getId());
            statement.setInt(2, planning.getAgentId());
            statement.setInt(3, planning.getSiteId());
            statement.setDate(4, Date.valueOf(planning.getDateService()));
            statement.setTime(5, Time.valueOf(planning.getHeureDebut()));
            statement.setTime(6, Time.valueOf(planning.getHeureFin()));
            statement.setString(7, planning.getTypeService());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void update(Planning planning) {
        String sql = "UPDATE plannings SET agent_id=?, site_id=?, date_service=?, heure_debut=?, heure_fin=?, type_service=? WHERE id=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, planning.getAgentId());
            statement.setInt(2, planning.getSiteId());
            statement.setDate(3, Date.valueOf(planning.getDateService()));
            statement.setTime(4, Time.valueOf(planning.getHeureDebut()));
            statement.setTime(5, Time.valueOf(planning.getHeureFin()));
            statement.setString(6, planning.getTypeService());
            statement.setInt(7, planning.getId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void delete(int id) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM plannings WHERE id=?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }
}
