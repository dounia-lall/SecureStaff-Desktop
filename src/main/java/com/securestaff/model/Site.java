package com.securestaff.model;

import java.time.LocalDate;

public class Site {
    private int id;
    private String nom;
    private String adresse;
    private String client;
    private int agentsNecessaires;
    private int agentsJour;
    private int agentsNuit;
    private LocalDate dateBesoin;

    public Site(int id, String nom, String adresse, String client, int agentsNecessaires) {
        this(id, nom, adresse, client, agentsNecessaires, agentsNecessaires, 0, null);
    }

    public Site(int id, String nom, String adresse, String client, int agentsNecessaires, int agentsJour, int agentsNuit, LocalDate dateBesoin) {
        this.id = id;
        this.nom = nom;
        this.adresse = adresse;
        this.client = client;
        this.agentsJour = agentsJour;
        this.agentsNuit = agentsNuit;
        this.agentsNecessaires = agentsJour + agentsNuit > 0 ? agentsJour + agentsNuit : agentsNecessaires;
        this.dateBesoin = dateBesoin;
    }

    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getAdresse() { return adresse; }
    public String getClient() { return client; }
    public int getAgentsNecessaires() { return agentsNecessaires; }
    public int getAgentsJour() { return agentsJour; }
    public int getAgentsNuit() { return agentsNuit; }
    public LocalDate getDateBesoin() { return dateBesoin; }

    @Override
    public String toString() {
        return id + " - " + nom;
    }
}
