package com.securestaff.dao;

import com.securestaff.db.Database;
import com.securestaff.model.Site;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SiteDao {
    public List<Site> findAll() {
        List<Site> sites = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM sites ORDER BY id DESC");
            ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Date date = readDate(rs, "date_besoin");
                int agentsNecessaires = rs.getInt("agents_necessaires");
                int agentsJour = readInt(rs, "agents_jour", agentsNecessaires);
                int agentsNuit = readInt(rs, "agents_nuit", 0);
                sites.add(new Site(rs.getInt("id"), rs.getString("nom"), rs.getString("adresse"), rs.getString("client"),
                        agentsNecessaires, agentsJour, agentsNuit, date == null ? null : date.toLocalDate()));
            }
            return sites;
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void save(Site site) {
        String sql = "INSERT INTO sites (nom, adresse, client, agents_necessaires, agents_jour, agents_nuit, date_besoin) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fill(statement, site);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void restore(Site site) {
        String sql = "INSERT INTO sites (id, nom, adresse, client, agents_necessaires, agents_jour, agents_nuit, date_besoin) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, site.getId());
            statement.setString(2, site.getNom());
            statement.setString(3, site.getAdresse());
            statement.setString(4, site.getClient());
            statement.setInt(5, site.getAgentsNecessaires());
            statement.setInt(6, site.getAgentsJour());
            statement.setInt(7, site.getAgentsNuit());
            statement.setDate(8, site.getDateBesoin() == null ? null : Date.valueOf(site.getDateBesoin()));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void update(Site site) {
        String sql = "UPDATE sites SET nom=?, adresse=?, client=?, agents_necessaires=?, agents_jour=?, agents_nuit=?, date_besoin=? WHERE id=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fill(statement, site);
            statement.setInt(8, site.getId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void delete(int id) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM sites WHERE id=?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    private void fill(PreparedStatement statement, Site site) throws SQLException {
        statement.setString(1, site.getNom());
        statement.setString(2, site.getAdresse());
        statement.setString(3, site.getClient());
        statement.setInt(4, site.getAgentsNecessaires());
        statement.setInt(5, site.getAgentsJour());
        statement.setInt(6, site.getAgentsNuit());
        statement.setDate(7, site.getDateBesoin() == null ? null : Date.valueOf(site.getDateBesoin()));
    }

    private int readInt(ResultSet rs, String column, int fallback) {
        try {
            return rs.getInt(column);
        } catch (SQLException exception) {
            return fallback;
        }
    }

    private Date readDate(ResultSet rs, String column) {
        try {
            return rs.getDate(column);
        } catch (SQLException exception) {
            return null;
        }
    }
}
