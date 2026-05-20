package com.securestaff.dao;

import com.securestaff.db.Database;
import com.securestaff.model.Presence;

import java.sql.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class PresenceDao {
    public List<Presence> findAll() {
        List<Presence> presences = new ArrayList<>();
        String sql = """
                SELECT pr.*, CONCAT(p.date_service, ' - ', a.prenom, ' ', a.nom, ' / ', s.nom) AS planning_label
                FROM presences pr
                JOIN plannings p ON p.id = pr.planning_id
                JOIN agents a ON a.id = p.agent_id
                JOIN sites s ON s.id = p.site_id
                ORDER BY pr.id DESC
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Time arrivee = rs.getTime("heure_arrivee");
                Time depart = rs.getTime("heure_depart");
                presences.add(new Presence(
                        rs.getInt("id"), rs.getInt("planning_id"), rs.getString("planning_label"),
                        rs.getString("statut"),
                        arrivee == null ? null : arrivee.toLocalTime(),
                        depart == null ? null : depart.toLocalTime()
                ));
            }
            return presences;
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public Presence findByPlanningId(int planningId) {
        return findAll().stream().filter(p -> p.getPlanningId() == planningId).findFirst().orElse(null);
    }

    public void mark(int planningId, String statut, LocalTime arrivee, LocalTime depart) {
        String sql = """
                INSERT INTO presences (planning_id, statut, heure_arrivee, heure_depart)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE statut=VALUES(statut), heure_arrivee=VALUES(heure_arrivee), heure_depart=VALUES(heure_depart)
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, planningId);
            statement.setString(2, statut);
            statement.setTime(3, arrivee == null ? null : Time.valueOf(arrivee));
            statement.setTime(4, depart == null ? null : Time.valueOf(depart));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void update(Presence presence, String statut, LocalTime arrivee, LocalTime depart) {
        String sql = "UPDATE presences SET statut=?, heure_arrivee=?, heure_depart=? WHERE id=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, statut);
            statement.setTime(2, arrivee == null ? null : Time.valueOf(arrivee));
            statement.setTime(3, depart == null ? null : Time.valueOf(depart));
            statement.setInt(4, presence.getId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void restore(Presence presence) {
        mark(presence.getPlanningId(), presence.getStatut(), presence.getHeureArrivee(), presence.getHeureDepart());
    }

    public void delete(int id) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM presences WHERE id=?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void deleteByPlanningId(int planningId) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM presences WHERE planning_id=?")) {
            statement.setInt(1, planningId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }
}
