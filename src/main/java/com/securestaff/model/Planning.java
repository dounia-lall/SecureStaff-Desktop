package com.securestaff.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Planning {
    private int id;
    private int agentId;
    private String agent;
    private int siteId;
    private String site;
    private String siteAdresse;
    private LocalDate dateService;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private String typeService;

    public Planning(int id, int agentId, String agent, int siteId, String site, LocalDate dateService, LocalTime heureDebut, LocalTime heureFin, String typeService) {
        this(id, agentId, agent, siteId, site, "", dateService, heureDebut, heureFin, typeService);
    }

    public Planning(int id, int agentId, String agent, int siteId, String site, String siteAdresse, LocalDate dateService, LocalTime heureDebut, LocalTime heureFin, String typeService) {
        this.id = id;
        this.agentId = agentId;
        this.agent = agent;
        this.siteId = siteId;
        this.site = site;
        this.siteAdresse = siteAdresse;
        this.dateService = dateService;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.typeService = typeService;
    }

    public int getId() { return id; }
    public int getAgentId() { return agentId; }
    public String getAgent() { return agent; }
    public int getSiteId() { return siteId; }
    public String getSite() { return site; }
    public String getSiteAdresse() { return siteAdresse; }
    public LocalDate getDateService() { return dateService; }
    public LocalTime getHeureDebut() { return heureDebut; }
    public LocalTime getHeureFin() { return heureFin; }
    public String getTypeService() { return typeService; }

    @Override
    public String toString() {
        return id + " - " + dateService + " - " + agent + " / " + site;
    }
}
