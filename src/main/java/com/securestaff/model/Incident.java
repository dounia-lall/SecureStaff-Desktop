package com.securestaff.model;

import java.time.LocalDateTime;

public class Incident {
    private int id;
    private int siteId;
    private String site;
    private int agentId;
    private String agent;
    private String description;
    private String gravite;
    private LocalDateTime dateIncident;

    public Incident(int id, String site, String agent, String description, String gravite, LocalDateTime dateIncident) {
        this(id, 0, site, 0, agent, description, gravite, dateIncident);
    }

    public Incident(int id, int siteId, String site, int agentId, String agent, String description, String gravite, LocalDateTime dateIncident) {
        this.id = id;
        this.siteId = siteId;
        this.site = site;
        this.agentId = agentId;
        this.agent = agent;
        this.description = description;
        this.gravite = gravite;
        this.dateIncident = dateIncident;
    }

    public int getId() { return id; }
    public int getSiteId() { return siteId; }
    public String getSite() { return site; }
    public int getAgentId() { return agentId; }
    public String getAgent() { return agent; }
    public String getDescription() { return description; }
    public String getGravite() { return gravite; }
    public LocalDateTime getDateIncident() { return dateIncident; }
}
