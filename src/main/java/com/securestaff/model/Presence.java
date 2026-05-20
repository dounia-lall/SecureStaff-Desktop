package com.securestaff.model;

import java.time.LocalTime;

public class Presence {
    private int id;
    private int planningId;
    private String planning;
    private String statut;
    private LocalTime heureArrivee;
    private LocalTime heureDepart;

    public Presence(int id, int planningId, String planning, String statut, LocalTime heureArrivee, LocalTime heureDepart) {
        this.id = id;
        this.planningId = planningId;
        this.planning = planning;
        this.statut = statut;
        this.heureArrivee = heureArrivee;
        this.heureDepart = heureDepart;
    }

    public int getId() { return id; }
    public int getPlanningId() { return planningId; }
    public String getPlanning() { return planning; }
    public String getStatut() { return statut; }
    public LocalTime getHeureArrivee() { return heureArrivee; }
    public LocalTime getHeureDepart() { return heureDepart; }
}
