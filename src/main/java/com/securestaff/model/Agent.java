package com.securestaff.model;

import java.time.LocalDate;

public class Agent {
    private int id;
    private String nom;
    private String prenom;
    private String telephone;
    private String email;
    private String poste;
    private String statut;
    private LocalDate dateEmbauche;
    private LocalDate dateNaissance;
    private String sexe;
    private String documentPath;
    private String typeContrat;

    public Agent(int id, String nom, String prenom, String telephone, String email, String poste, String statut, LocalDate dateEmbauche) {
        this(id, nom, prenom, telephone, email, poste, statut, dateEmbauche, null, "", "", "");
    }

    public Agent(int id, String nom, String prenom, String telephone, String email, String poste, String statut, LocalDate dateEmbauche, LocalDate dateNaissance, String sexe, String documentPath) {
        this(id, nom, prenom, telephone, email, poste, statut, dateEmbauche, dateNaissance, sexe, documentPath, "");
    }

    public Agent(int id, String nom, String prenom, String telephone, String email, String poste, String statut, LocalDate dateEmbauche, LocalDate dateNaissance, String sexe, String documentPath, String typeContrat) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.email = email;
        this.poste = poste;
        this.statut = statut;
        this.dateEmbauche = dateEmbauche;
        this.dateNaissance = dateNaissance;
        this.sexe = sexe;
        this.documentPath = documentPath;
        this.typeContrat = typeContrat;
    }

    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getTelephone() { return telephone; }
    public String getEmail() { return email; }
    public String getPoste() { return poste; }
    public String getStatut() { return statut; }
    public LocalDate getDateEmbauche() { return dateEmbauche; }
    public LocalDate getDateNaissance() { return dateNaissance; }
    public String getSexe() { return sexe; }
    public String getDocumentPath() { return documentPath; }
    public String getTypeContrat() { return typeContrat; }

    @Override
    public String toString() {
        return id + " - " + prenom + " " + nom;
    }
}
