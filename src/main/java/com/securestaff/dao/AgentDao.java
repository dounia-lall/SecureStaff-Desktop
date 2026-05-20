package com.securestaff.dao;

import com.securestaff.db.Database;
import com.securestaff.model.Agent;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AgentDao {
    public List<Agent> findAll() {
        List<Agent> agents = new ArrayList<>();
        String sql = "SELECT * FROM agents ORDER BY id DESC";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Date date = rs.getDate("date_embauche");
                Date naissance = readDate(rs, "date_naissance");
                agents.add(new Agent(
                        rs.getInt("id"), rs.getString("nom"), rs.getString("prenom"),
                        rs.getString("telephone"), rs.getString("email"), rs.getString("poste"),
                        rs.getString("statut"), date == null ? null : date.toLocalDate(),
                        naissance == null ? null : naissance.toLocalDate(),
                        readString(rs, "sexe"),
                        readString(rs, "document_path"),
                        readString(rs, "type_contrat")
                ));
            }
            return agents;
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void save(Agent agent) {
        String sql = "INSERT INTO agents (nom, prenom, telephone, email, poste, statut, date_embauche, date_naissance, sexe, document_path, type_contrat) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fill(statement, agent);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void restore(Agent agent) {
        String sql = "INSERT INTO agents (id, nom, prenom, telephone, email, poste, statut, date_embauche, date_naissance, sexe, document_path, type_contrat) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, agent.getId());
            statement.setString(2, agent.getNom());
            statement.setString(3, agent.getPrenom());
            statement.setString(4, agent.getTelephone());
            statement.setString(5, agent.getEmail());
            statement.setString(6, agent.getPoste());
            statement.setString(7, agent.getStatut());
            statement.setDate(8, agent.getDateEmbauche() == null ? null : Date.valueOf(agent.getDateEmbauche()));
            statement.setDate(9, agent.getDateNaissance() == null ? null : Date.valueOf(agent.getDateNaissance()));
            statement.setString(10, agent.getSexe());
            statement.setString(11, agent.getDocumentPath());
            statement.setString(12, agent.getTypeContrat());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void update(Agent agent) {
        String sql = "UPDATE agents SET nom=?, prenom=?, telephone=?, email=?, poste=?, statut=?, date_embauche=?, date_naissance=?, sexe=?, document_path=?, type_contrat=? WHERE id=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fill(statement, agent);
            statement.setInt(12, agent.getId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void delete(int id) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM agents WHERE id=?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    private void fill(PreparedStatement statement, Agent agent) throws SQLException {
        statement.setString(1, agent.getNom());
        statement.setString(2, agent.getPrenom());
        statement.setString(3, agent.getTelephone());
        statement.setString(4, agent.getEmail());
        statement.setString(5, agent.getPoste());
        statement.setString(6, agent.getStatut());
        statement.setDate(7, agent.getDateEmbauche() == null ? null : Date.valueOf(agent.getDateEmbauche()));
        statement.setDate(8, agent.getDateNaissance() == null ? null : Date.valueOf(agent.getDateNaissance()));
        statement.setString(9, agent.getSexe());
        statement.setString(10, agent.getDocumentPath());
        statement.setString(11, agent.getTypeContrat());
    }

    private Date readDate(ResultSet rs, String column) {
        try {
            return rs.getDate(column);
        } catch (SQLException exception) {
            return null;
        }
    }

    private String readString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException exception) {
            return "";
        }
    }
}
