package com.securestaff;

import com.securestaff.dao.*;
import com.securestaff.db.Database;
import com.securestaff.model.*;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;

public class MainApp extends Application {
    private final AdminDao adminDao = new AdminDao();
    private final AgentDao agentDao = new AgentDao();
    private final SiteDao siteDao = new SiteDao();
    private final PlanningDao planningDao = new PlanningDao();
    private final PresenceDao presenceDao = new PresenceDao();
    private final IncidentDao incidentDao = new IncidentDao();

    private final List<Runnable> refreshers = new ArrayList<>();
    private final ArrayDeque<Runnable> undoStack = new ArrayDeque<>();
    private final ArrayDeque<String> actionHistory = new ArrayDeque<>();
    private Button undoButton;
    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("SecureStaff Desktop");
        Database.initializeSchema();
        showLogin();
        stage.show();
    }

    private void showLogin() {
        Label brand = new Label("SecureStaff Desktop");
        brand.getStyleClass().add("brand");
        Label subtitle = new Label("Gestion desktop des agents, sites, plannings, presences et incidents.");
        subtitle.getStyleClass().add("subtitle");

        TextField username = new TextField("admin");
        username.setPromptText("Login admin");
        PasswordField password = new PasswordField();
        password.setPromptText("Mot de passe");
        password.setText("admin123");

        Label error = new Label();
        error.setStyle("-fx-text-fill:#dc2626;-fx-font-weight:700;");

        Button login = new Button("Se connecter");
        login.getStyleClass().add("primary-button");
        login.setMaxWidth(Double.MAX_VALUE);
        login.setOnAction(event -> {
            try {
                if (adminDao.login(username.getText().trim(), password.getText())) {
                    showDashboard();
                } else {
                    error.setText("Identifiants incorrects.");
                }
            } catch (RuntimeException exception) {
                error.setText(exception.getMessage());
            }
        });

        VBox card = new VBox(14, brand, subtitle, username, password, login, error);
        card.getStyleClass().add("login-card");
        card.setPadding(new Insets(34));
        card.setMaxWidth(430);

        stage.setScene(scene(new StackPane(card), 980, 640));
    }

    private void showDashboard() {
        refreshers.clear();
        undoStack.clear();

        Label title = new Label("Tableau de bord admin");
        title.getStyleClass().add("page-title");
        Label info = new Label("Admin connecte - vous gerez l'ensemble de SecureStaff Desktop.");
        info.getStyleClass().add("subtitle");

        undoButton = new Button("Retour arriere");
        undoButton.getStyleClass().add("muted-button");
        undoButton.setDisable(true);
        undoButton.setOnAction(event -> safe(() -> {
            Runnable undo = undoStack.pollLast();
            if (undo != null) {
                undo.run();
                refreshAll();
            }
            undoButton.setDisable(undoStack.isEmpty());
        }));

        Button logout = new Button("Deconnexion");
        logout.getStyleClass().add("muted-button");
        logout.setOnAction(event -> showLogin());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label logo = new Label("SS");
        logo.getStyleClass().add("app-logo");
        HBox header = new HBox(14, logo, new VBox(4, title, info), spacer, undoButton, logout);
        header.setAlignment(Pos.CENTER_LEFT);

        TabPane tabs = new TabPane(
                tab("Dashboard", dashboardView()),
                tab("Agents", agentsView()),
                tab("Sites", sitesView()),
                tab("Planning", planningView()),
                tab("Presence", presenceView()),
                tab("Incidents", incidentsView()),
                tab("Planning mensuel", monthlyPlanningView())
        );
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> refreshAll());

        VBox root = new VBox(22, header, tabs);
        root.setPadding(new Insets(24));
        stage.setScene(scene(root, 1360, 820));
        refreshAll();
    }

    private Tab tab(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title);
        tab.setContent(content);
        return tab;
    }

    private javafx.scene.Node dashboardView() {
        VBox container = new VBox(18);
        container.getStyleClass().add("panel");
        container.setPadding(new Insets(18));

        DatePicker monthPicker = new DatePicker(LocalDate.now().withDayOfMonth(1));
        configureDatePicker(monthPicker);
        Label monthLabel = new Label("Mois analyse");
        monthLabel.getStyleClass().add("section-title");
        HBox monthBar = new HBox(12, monthLabel, monthPicker);
        monthBar.setAlignment(Pos.CENTER_LEFT);

        HBox cards = new HBox(14);
        cards.setAlignment(Pos.CENTER_LEFT);

        Label activeAgents = statCard("Agents actifs", "0", "#dbeafe", "#1d4ed8");
        Label absentAgents = statCard("Agents absents", "0", "#fee2e2", "#b91c1c");
        Label todayIncidents = statCard("Incidents aujourd'hui", "0", "#fef3c7", "#92400e");
        Label coveredSites = statCard("Couverture des sites", "0%", "#dcfce7", "#15803d");
        cards.getChildren().addAll(activeAgents, absentAgents, todayIncidents, coveredSites);

        VBox alertsBox = new VBox(8);
        alertsBox.getStyleClass().add("alert-panel");
        Label alertsTitle = new Label("Alertes du jour");
        alertsTitle.getStyleClass().add("section-title");
        alertsBox.getChildren().add(alertsTitle);

        GridPane sitesTodayTable = dashboardGrid("Site", "Date", "Jour", "Nuit", "Couverture");
        GridPane historyTable = dashboardGrid("Heure", "Action");

        CategoryAxis presenceAxis = new CategoryAxis();
        NumberAxis presenceValueAxis = new NumberAxis();
        presenceAxis.setLabel("Jour du mois");
        presenceValueAxis.setLabel("Nombre");
        presenceValueAxis.setForceZeroInRange(true);
        presenceValueAxis.setMinorTickVisible(false);
        presenceValueAxis.setTickUnit(1);
        BarChart<String, Number> presenceChart = new BarChart<>(presenceAxis, presenceValueAxis);
        presenceChart.setTitle("Presence par jour");
        presenceChart.setLegendVisible(true);
        presenceChart.setCategoryGap(8);
        presenceChart.setBarGap(2);
        presenceChart.setPrefHeight(320);
        presenceChart.setMinHeight(300);
        presenceChart.getStyleClass().add("dashboard-chart");
        presenceChart.getStyleClass().add("presence-histogram");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Type de contrat");
        yAxis.setLabel("Agents actifs");
        yAxis.setForceZeroInRange(true);
        BarChart<String, Number> planningChart = new BarChart<>(xAxis, yAxis);
        planningChart.setTitle("Agents actifs par contrat");
        planningChart.setLegendVisible(false);
        planningChart.setPrefHeight(320);
        planningChart.setMinHeight(300);
        planningChart.getStyleClass().add("dashboard-chart");
        planningChart.getStyleClass().add("contract-chart");

        Runnable refresh = () -> {
            List<Agent> agents = agentDao.findAll();
            List<Presence> presences = presenceDao.findAll();
            List<Incident> incidents = incidentDao.findAll();
            List<Site> sites = siteDao.findAll();
            List<Planning> plannings = planningDao.findAll();
            LocalDate selectedMonth = monthPicker.getValue() == null ? LocalDate.now().withDayOfMonth(1) : monthPicker.getValue().withDayOfMonth(1);
            YearMonth yearMonth = YearMonth.from(selectedMonth);
            LocalDate today = LocalDate.now();

            Map<Integer, Planning> planningById = new HashMap<>();
            for (Planning planning : plannings) {
                planningById.put(planning.getId(), planning);
            }
            List<Planning> monthPlannings = plannings.stream()
                    .filter(p -> YearMonth.from(p.getDateService()).equals(yearMonth))
                    .toList();
            List<Integer> plannedAgentIds = monthPlannings.stream().map(Planning::getAgentId).distinct().toList();
            List<Agent> activeMonthAgents = agents.stream()
                    .filter(a -> plannedAgentIds.contains(a.getId()))
                    .filter(a -> "Actif".equalsIgnoreCase(a.getStatut()))
                    .toList();
            long active = activeMonthAgents.size();
            List<Presence> monthPresences = presences.stream()
                    .filter(p -> planningById.containsKey(p.getPlanningId()))
                    .filter(p -> YearMonth.from(planningById.get(p.getPlanningId()).getDateService()).equals(yearMonth))
                    .toList();
            long absent = monthPresences.stream()
                    .filter(p -> "Absent".equalsIgnoreCase(p.getStatut()))
                    .count();
            List<Incident> todayIncidentsList = incidents.stream().filter(i -> i.getDateIncident().toLocalDate().equals(today)).toList();
            long incidentsToday = todayIncidentsList.size();
            List<Planning> todayRows = plannings.stream().filter(p -> p.getDateService().equals(today)).toList();
            List<Site> dashboardSites = sitesForDashboard(sites, monthPlannings, yearMonth);
            long covered = dashboardSites.stream().filter(site -> isSiteExactlyCovered(site, rowsForSiteDate(site, monthPlannings))).count();
            int coverage = dashboardSites.isEmpty() ? 0 : (int) Math.round((covered * 100.0) / dashboardSites.size());

            activeAgents.setText("Agents actifs\n" + active);
            absentAgents.setText("Agents absents\n" + absent);
            todayIncidents.setText("Incidents aujourd'hui\n" + incidentsToday);
            coveredSites.setText("Couverture des sites\n" + coverage + "%");

            XYChart.Series<String, Number> presentSeries = new XYChart.Series<>();
            presentSeries.setName("Present");
            XYChart.Series<String, Number> absentSeries = new XYChart.Series<>();
            absentSeries.setName("Absent");
            XYChart.Series<String, Number> lateSeries = new XYChart.Series<>();
            lateSeries.setName("Retard");
            Set<Integer> daysToDisplay = new TreeSet<>();
            int maxPresenceValue = 1;
            for (Presence presence : monthPresences) {
                Planning planning = planningById.get(presence.getPlanningId());
                if (planning != null) daysToDisplay.add(planning.getDateService().getDayOfMonth());
            }
            if (daysToDisplay.isEmpty()) {
                monthPlannings.forEach(planning -> daysToDisplay.add(planning.getDateService().getDayOfMonth()));
            }
            for (int day : daysToDisplay) {
                LocalDate current = yearMonth.atDay(day);
                long dayPresent = monthPresences.stream()
                        .filter(p -> planningById.containsKey(p.getPlanningId()) && planningById.get(p.getPlanningId()).getDateService().equals(current))
                        .filter(p -> "Present".equalsIgnoreCase(p.getStatut())).count();
                long dayAbsent = monthPresences.stream()
                        .filter(p -> planningById.containsKey(p.getPlanningId()) && planningById.get(p.getPlanningId()).getDateService().equals(current))
                        .filter(p -> "Absent".equalsIgnoreCase(p.getStatut())).count();
                long dayLate = monthPresences.stream()
                        .filter(p -> planningById.containsKey(p.getPlanningId()) && planningById.get(p.getPlanningId()).getDateService().equals(current))
                        .filter(p -> "Retard".equalsIgnoreCase(p.getStatut())).count();
                maxPresenceValue = Math.max(maxPresenceValue, (int) Math.max(dayPresent, Math.max(dayAbsent, dayLate)));
                presentSeries.getData().add(new XYChart.Data<>(String.valueOf(day), dayPresent));
                absentSeries.getData().add(new XYChart.Data<>(String.valueOf(day), dayAbsent));
                lateSeries.getData().add(new XYChart.Data<>(String.valueOf(day), dayLate));
            }
            presenceValueAxis.setAutoRanging(false);
            presenceValueAxis.setLowerBound(0);
            presenceValueAxis.setUpperBound(maxPresenceValue + 1);
            presenceValueAxis.setTickUnit(1);
            presenceChart.getData().setAll(presentSeries, absentSeries, lateSeries);

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            for (String contract : List.of("CDI", "CDD", "Interim", "Stage")) {
                series.getData().add(new XYChart.Data<>(contract, activeMonthAgents.stream().filter(a -> contract.equalsIgnoreCase(a.getTypeContrat())).count()));
            }
            planningChart.getData().setAll(series);

            alertsBox.getChildren().setAll(alertsTitle);
            List<String> alerts = dashboardAlerts(today, dashboardSites, monthPlannings, monthPresences, todayIncidentsList, planningById);
            if (alerts.isEmpty()) {
                alertsBox.getChildren().add(alertLabel("Aucune alerte aujourd'hui."));
            } else {
                alerts.forEach(message -> alertsBox.getChildren().add(alertLabel(message)));
            }

            fillSitesTodayTable(sitesTodayTable, dashboardSites, monthPlannings);
            fillHistoryTable(historyTable);
        };
        register(refresh);
        monthPicker.setOnAction(event -> refresh.run());

        VBox presenceCard = chartCard(presenceChart);
        VBox planningCard = chartCard(planningChart);
        HBox charts = new HBox(18, presenceCard, planningCard);
        HBox.setHgrow(presenceCard, Priority.ALWAYS);
        HBox.setHgrow(planningCard, Priority.ALWAYS);
        HBox dashboardTables = new HBox(18, new VBox(8, titleLabel("Sites du jour"), sitesTodayTable), new VBox(8, titleLabel("Historique actions"), historyTable));
        container.getChildren().addAll(monthBar, cards, alertsBox, dashboardTables, charts);
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        return scrollPane;
    }

    private javafx.scene.Node agentsView() {
        TableView<Agent> table = table();
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        TableColumn<Agent, String> documentColumn = new TableColumn<>("Document");
        documentColumn.setPrefWidth(240);
        documentColumn.setCellValueFactory(data -> new SimpleStringProperty(fileName(data.getValue().getDocumentPath())));
        table.getColumns().addAll(
                column("ID", "id", 60), column("Nom", "nom", 120), column("Prenom", "prenom", 120),
                column("Telephone", "telephone", 130), column("Email", "email", 210),
                column("Poste", "poste", 150), column("Statut", "statut", 90), column("Embauche", "dateEmbauche", 115),
                column("Naissance", "dateNaissance", 115), column("Sexe", "sexe", 80), column("Contrat", "typeContrat", 90), documentColumn
        );

        TextField nom = field("Nom");
        TextField prenom = field("Prenom");
        TextField telephone = field("Telephone");
        TextField email = field("Email");
        TextField poste = field("Poste");
        ComboBox<String> statut = combo("Actif", "Inactif", "Conge");
        colorCombo(statut, this::statusColor);
        DatePicker embauche = new DatePicker(LocalDate.now());
        DatePicker naissance = new DatePicker();
        configureDatePicker(embauche);
        configureDatePicker(naissance);
        naissance.setPromptText("Date de naissance");
        ComboBox<String> sexe = combo("Femme", "Homme", "Autre");
        ComboBox<String> typeContrat = combo("CDI", "CDD", "Interim", "Stage");
        colorCombo(typeContrat, this::contractColor);
        TextField documentPath = field("Document agent");
        Button browseDocument = action("Choisir document", () -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Selectionner un document agent");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documents PDF", "*.pdf"));
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                if (isPdf(file.toPath())) {
                    documentPath.setText(file.getAbsolutePath());
                } else {
                    warning("Document invalide", "Le document agent doit etre un fichier PDF.");
                }
            }
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                nom.setText(selected.getNom());
                prenom.setText(selected.getPrenom());
                telephone.setText(selected.getTelephone());
                email.setText(selected.getEmail());
                poste.setText(selected.getPoste());
                statut.setValue(selected.getStatut());
                embauche.setValue(selected.getDateEmbauche());
                naissance.setValue(selected.getDateNaissance());
                sexe.setValue(selected.getSexe() == null || selected.getSexe().isBlank() ? "Femme" : selected.getSexe());
                typeContrat.setValue(selected.getTypeContrat() == null || selected.getTypeContrat().isBlank() ? "CDI" : selected.getTypeContrat());
                documentPath.setText(selected.getDocumentPath());
            }
        });
        table.setRowFactory(view -> new TableRow<>() {
            @Override
            protected void updateItem(Agent agent, boolean empty) {
                super.updateItem(agent, empty);
                if (empty || agent == null) {
                    setStyle("");
                } else if ("Conge".equalsIgnoreCase(agent.getStatut())) {
                    setStyle("-fx-background-color:#ffedd5;-fx-text-background-color:#c2410c;");
                } else if ("Inactif".equalsIgnoreCase(agent.getStatut())) {
                    setStyle("-fx-background-color:#fee2e2;-fx-text-background-color:#b91c1c;");
                } else if ("CDD".equalsIgnoreCase(agent.getTypeContrat())) {
                    setStyle("-fx-background-color:#fef9c3;-fx-text-background-color:#a16207;");
                } else if ("CDI".equalsIgnoreCase(agent.getTypeContrat())) {
                    setStyle("-fx-background-color:#dcfce7;-fx-text-background-color:#15803d;");
                } else {
                    setStyle("");
                }
            }
        });
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Agent selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openDocument(selected.getDocumentPath());
                }
            }
        });

        TextField search = field("Rechercher un agent...");
        Runnable refresh = () -> setFilteredItems(table, search, agentDao.findAll(), agent ->
                contains(agent.getNom(), search.getText()) || contains(agent.getPrenom(), search.getText()) ||
                        contains(agent.getEmail(), search.getText()) || contains(agent.getTelephone(), search.getText()) ||
                        contains(agent.getPoste(), search.getText()) || contains(agent.getStatut(), search.getText()));
        register(refresh);
        search.textProperty().addListener((obs, old, value) -> refresh.run());

        Button add = action("Ajouter", () -> {
            if (!validateAgentForm(null, nom.getText(), prenom.getText(), telephone.getText(), email.getText(), naissance.getValue())) return;
            agentDao.save(new Agent(0, nom.getText(), prenom.getText(), telephone.getText(), email.getText(), poste.getText(), statut.getValue(), embauche.getValue(), naissance.getValue(), sexe.getValue(), storeDocument(documentPath.getText()), typeContrat.getValue()));
            rememberAction(prenom.getText() + " ajoutee aux agents");
            clearAgentForm(table, nom, prenom, telephone, email, poste, documentPath, statut, sexe, typeContrat, embauche, naissance);
            refreshAll();
        });
        Button edit = action("Modifier", () -> {
            Agent old = table.getSelectionModel().getSelectedItem();
            if (old == null) return;
            if (!validateAgentForm(old.getId(), nom.getText(), prenom.getText(), telephone.getText(), email.getText(), naissance.getValue())) return;
            Agent updated = new Agent(old.getId(), nom.getText(), prenom.getText(), telephone.getText(), email.getText(), poste.getText(), statut.getValue(), embauche.getValue(), naissance.getValue(), sexe.getValue(), storeDocument(documentPath.getText()), typeContrat.getValue());
            agentDao.update(updated);
            pushUndo(() -> agentDao.update(old));
            rememberAction(prenom.getText() + " modifiee");
            clearAgentForm(table, nom, prenom, telephone, email, poste, documentPath, statut, sexe, typeContrat, embauche, naissance);
            refreshAll();
        });
        Button delete = danger("Supprimer", () -> {
            List<Agent> selectedAgents = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedAgents.isEmpty()) return;
            for (Agent selected : selectedAgents) {
                agentDao.delete(selected.getId());
            }
            pushUndo(() -> selectedAgents.forEach(agentDao::restore));
            rememberAction(selectedAgents.size() == 1 ? selectedAgents.get(0).getPrenom() + " supprimee" : selectedAgents.size() + " agents supprimes");
            clearAgentForm(table, nom, prenom, telephone, email, poste, documentPath, statut, sexe, typeContrat, embauche, naissance);
            refreshAll();
        });

        return panel(new VBox(12, search, new GridPaneBuilder().add(nom, prenom, telephone, email, poste, statut, typeContrat, embauche, naissance, sexe, documentPath, browseDocument).build()), new HBox(10, add, edit, delete), table);
    }

    private javafx.scene.Node sitesView() {
        TableView<Site> table = table();
        table.getColumns().addAll(
                column("ID", "id", 60), column("Nom du site", "nom", 200), column("Adresse", "adresse", 360),
                column("Client", "client", 180), column("Jour", "agentsJour", 90),
                column("Nuit", "agentsNuit", 90), column("Date", "dateBesoin", 120)
        );

        TextField nom = field("Nom du site");
        TextField adresse = field("Adresse");
        TextField client = field("Client");
        TextField agentsJour = field("Agents jour");
        TextField agentsNuit = field("Agents nuit");
        DatePicker dateBesoin = new DatePicker(LocalDate.now());
        configureDatePicker(dateBesoin);

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                nom.setText(selected.getNom());
                adresse.setText(selected.getAdresse());
                client.setText(selected.getClient());
                agentsJour.setText(String.valueOf(selected.getAgentsJour()));
                agentsNuit.setText(String.valueOf(selected.getAgentsNuit()));
                dateBesoin.setValue(selected.getDateBesoin());
            }
        });

        TextField search = field("Rechercher un site...");
        table.setRowFactory(view -> new TableRow<>() {
            @Override
            protected void updateItem(Site siteItem, boolean empty) {
                super.updateItem(siteItem, empty);
                setStyle(empty || siteItem == null ? "" : "-fx-background-color:" + siteColor(siteItem.getId(), 0.14) + ";-fx-text-background-color:" + siteTextColor(siteItem.getId()) + ";");
            }
        });
        Runnable refresh = () -> setFilteredItems(table, search, siteDao.findAll(), siteItem ->
                contains(siteItem.getNom(), search.getText()) || contains(siteItem.getAdresse(), search.getText()) ||
                        contains(siteItem.getClient(), search.getText()));
        register(refresh);
        search.textProperty().addListener((obs, old, value) -> refresh.run());

        Button add = action("Ajouter", () -> {
            int jour = integer(agentsJour.getText(), 0);
            int nuit = integer(agentsNuit.getText(), 0);
            siteDao.save(new Site(0, nom.getText(), adresse.getText(), client.getText(), jour + nuit, jour, nuit, dateBesoin.getValue()));
            rememberAction("Site ajoute : " + nom.getText());
            clearSiteForm(table, nom, adresse, client, agentsJour, agentsNuit, dateBesoin);
            refreshAll();
        });
        Button edit = action("Modifier", () -> {
            Site old = table.getSelectionModel().getSelectedItem();
            if (old == null) return;
            int jour = integer(agentsJour.getText(), 0);
            int nuit = integer(agentsNuit.getText(), 0);
            siteDao.update(new Site(old.getId(), nom.getText(), adresse.getText(), client.getText(), jour + nuit, jour, nuit, dateBesoin.getValue()));
            pushUndo(() -> siteDao.update(old));
            rememberAction("Site modifie : " + nom.getText());
            clearSiteForm(table, nom, adresse, client, agentsJour, agentsNuit, dateBesoin);
            refreshAll();
        });
        Button delete = danger("Supprimer", () -> {
            Site selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            siteDao.delete(selected.getId());
            pushUndo(() -> siteDao.restore(selected));
            rememberAction("Site supprime : " + selected.getNom());
            clearSiteForm(table, nom, adresse, client, agentsJour, agentsNuit, dateBesoin);
            refreshAll();
        });

        return panel(new VBox(12, search, new GridPaneBuilder().add(nom, adresse, client, agentsJour, agentsNuit, dateBesoin).build()), new HBox(10, add, edit, delete), table);
    }

    private javafx.scene.Node planningView() {
        TableView<Planning> table = planningTable();
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        List<Agent> selectedAgentsForPlanning = new ArrayList<>();
        Label selectedAgentsLabel = new Label("Aucun agent selectionne");
        selectedAgentsLabel.getStyleClass().add("subtitle");
        ComboBox<Site> site = new ComboBox<>();
        DatePicker date = new DatePicker(LocalDate.now());
        configureDatePicker(date);
        TextField debut = field("Heure debut HH:mm");
        TextField fin = field("Heure fin HH:mm");
        ComboBox<String> type = combo("Jour", "Nuit");
        TextField search = field("Rechercher dans le planning...");
        Button chooseAgents = action("Choisir les agents", () -> {
            List<Agent> chosen = choosePlanningAgents(selectedAgentsForPlanning);
            if (chosen != null) {
                selectedAgentsForPlanning.clear();
                selectedAgentsForPlanning.addAll(chosen);
                selectedAgentsLabel.setText(chosen.isEmpty() ? "Aucun agent selectionne" : chosen.size() + " agent(s) selectionne(s) : " + chosen.stream().map(Agent::toString).reduce((a, b) -> a + ", " + b).orElse(""));
            }
        });

        Runnable refresh = () -> {
            site.setItems(FXCollections.observableArrayList(siteDao.findAll()));
            setFilteredItems(table, search, planningDao.findAll(), planning ->
                    contains(planning.getAgent(), search.getText()) || contains(planning.getSite(), search.getText()) ||
                            contains(planning.getSiteAdresse(), search.getText()) ||
                            contains(planning.getTypeService(), search.getText()) || contains(String.valueOf(planning.getDateService()), search.getText()));
        };
        register(refresh);
        search.textProperty().addListener((obs, old, value) -> refresh.run());

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                selectedAgentsForPlanning.clear();
                Agent selectedAgent = findAgent(agentDao.findAll(), selected.getAgentId());
                if (selectedAgent != null) selectedAgentsForPlanning.add(selectedAgent);
                selectedAgentsLabel.setText(selectedAgent == null ? "Aucun agent selectionne" : "1 agent selectionne : " + selectedAgent);
                site.setValue(findSite(site.getItems(), selected.getSiteId()));
                date.setValue(selected.getDateService());
                debut.setText(selected.getHeureDebut().toString());
                fin.setText(selected.getHeureFin().toString());
                type.setValue(selected.getTypeService());
            }
        });

        Button add = action("Affecter les agents selectionnes", () -> {
            List<Agent> selectedAgents = new ArrayList<>(selectedAgentsForPlanning);
            if (site.getValue() == null || selectedAgents.isEmpty()) return;
            LocalTime start = time(debut.getText());
            LocalTime end = time(fin.getText());
            if (!validatePlanningSelection(site.getValue(), selectedAgents, date.getValue(), start, end, type.getValue(), null, true)) {
                return;
            }
            for (Agent agent : selectedAgents) {
                planningDao.save(new Planning(0, agent.getId(), "", site.getValue().getId(), "", date.getValue(), start, end, type.getValue()));
                rememberAction(agent.getPrenom() + " ajoutee au planning");
            }
            selectedAgentsForPlanning.clear();
            selectedAgentsLabel.setText("Aucun agent selectionne");
            site.getSelectionModel().clearSelection();
            clear(debut, fin);
            type.getSelectionModel().selectFirst();
            refreshAll();
        });
        Button edit = action("Modifier", () -> {
            Planning selected = table.getSelectionModel().getSelectedItem();
            if (selected == null || site.getValue() == null) return;
            List<Agent> selectedAgents = new ArrayList<>(selectedAgentsForPlanning);
            if (selectedAgents.size() > 1) {
                warning("Modification impossible", "Pour modifier une ligne, selectionne un seul agent.");
                return;
            }
            Agent chosenAgent = selectedAgents.isEmpty() ? null : selectedAgents.get(0);
            int agentId = chosenAgent == null ? selected.getAgentId() : chosenAgent.getId();
            LocalTime start = time(debut.getText());
            LocalTime end = time(fin.getText());
            Agent finalAgent = chosenAgent == null ? findAgent(agentDao.findAll(), selected.getAgentId()) : chosenAgent;
            if (finalAgent == null || !validatePlanningSelection(site.getValue(), List.of(finalAgent), date.getValue(), start, end, type.getValue(), selected.getId(), false)) {
                return;
            }
            Planning updated = new Planning(selected.getId(), agentId, "", site.getValue().getId(), "", date.getValue(), start, end, type.getValue());
            planningDao.update(updated);
            pushUndo(() -> planningDao.update(selected));
            rememberAction("Planning modifie");
            selectedAgentsForPlanning.clear();
            selectedAgentsLabel.setText("Aucun agent selectionne");
            site.getSelectionModel().clearSelection();
            clear(debut, fin);
            type.getSelectionModel().selectFirst();
            table.getSelectionModel().clearSelection();
            refreshAll();
        });
        Button delete = danger("Supprimer", () -> {
            List<Planning> selectedRows = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedRows.isEmpty()) return;
            for (Planning selected : selectedRows) {
                planningDao.delete(selected.getId());
            }
            pushUndo(() -> selectedRows.forEach(planningDao::restore));
            rememberAction(selectedRows.size() == 1 ? "Planning supprime" : selectedRows.size() + " plannings supprimes");
            table.getSelectionModel().clearSelection();
            refreshAll();
        });

        site.setOnAction(event -> {
            Site selected = site.getValue();
            if (selected != null) {
                if (selected.getDateBesoin() != null) date.setValue(selected.getDateBesoin());
                applyDefaultPlanningHours(type.getValue(), debut, fin);
            }
        });
        type.setOnAction(event -> applyDefaultPlanningHours(type.getValue(), debut, fin));

        Label help = new Label("Choisis le site, la date et les horaires. Les agents se selectionnent dans une fenetre dediee.");
        help.getStyleClass().add("subtitle");
        GridPane form = new GridPaneBuilder().add(site, date, debut, fin, type).build();
        VBox left = new VBox(8, help, new HBox(10, chooseAgents, selectedAgentsLabel));
        return panel(new VBox(12, search, left, form), new HBox(10, add, edit, delete), table);
    }

    private javafx.scene.Node presenceView() {
        TableView<PresenceLine> table = table();
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getColumns().addAll(
                presenceColumn("Planning", PresenceLine::getPlanningIdLabel, 90),
                presenceColumn("Date", PresenceLine::getDate, 120),
                presenceColumn("Agent", PresenceLine::getAgent, 190),
                presenceColumn("Site", PresenceLine::getSite, 220),
                presenceColumn("Adresse site", PresenceLine::getSiteAdresse, 260),
                presenceColumn("Type", PresenceLine::getType, 90),
                presenceColumn("Horaires prevus", PresenceLine::getPlannedHours, 150),
                presenceColumn("Statut", PresenceLine::getStatut, 120),
                presenceColumn("Arrivee", PresenceLine::getArrivee, 110),
                presenceColumn("Depart", PresenceLine::getDepart, 110)
        );

        ComboBox<String> statut = combo("Present", "Absent", "Retard");
        colorCombo(statut, this::presenceColor);
        TextField arrivee = field("Heure arrivee HH:mm");
        TextField depart = field("Heure depart HH:mm");
        Label selectedInfo = new Label("Selectionne une affectation pour renseigner la presence.");
        selectedInfo.getStyleClass().add("subtitle");
        TextField search = field("Rechercher une présence...");
        table.setRowFactory(view -> new TableRow<>() {
            @Override
            protected void updateItem(PresenceLine line, boolean empty) {
                super.updateItem(line, empty);
                if (empty || line == null) setStyle("");
                else if ("Present".equalsIgnoreCase(line.getStatut())) setStyle("-fx-background-color:#dcfce7;-fx-text-background-color:#15803d;");
                else if ("Absent".equalsIgnoreCase(line.getStatut())) setStyle("-fx-background-color:#fee2e2;-fx-text-background-color:#b91c1c;");
                else if ("Retard".equalsIgnoreCase(line.getStatut())) setStyle("-fx-background-color:#fef3c7;-fx-text-background-color:#c2410c;");
                else setStyle("-fx-background-color:#f8fbff;-fx-text-background-color:#1f2a44;");
            }
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                statut.setValue(selected.getPresence() == null ? "Present" : selected.getStatut());
                arrivee.setText(selected.getPresence() == null || selected.getPresence().getHeureArrivee() == null ? selected.getPlanning().getHeureDebut().toString() : selected.getPresence().getHeureArrivee().toString());
                depart.setText(selected.getPresence() == null || selected.getPresence().getHeureDepart() == null ? selected.getPlanning().getHeureFin().toString() : selected.getPresence().getHeureDepart().toString());
                selectedInfo.setText(selected.getDate() + " - " + selected.getAgent() + " / " + selected.getSite() + " (" + selected.getType() + ")");
            }
        });

        Runnable refresh = () -> setFilteredItems(table, search, buildPresenceLines(), line ->
                contains(line.getAgent(), search.getText()) || contains(line.getSite(), search.getText()) ||
                        contains(line.getSiteAdresse(), search.getText()) ||
                        contains(line.getDate(), search.getText()) || contains(line.getType(), search.getText()) ||
                        contains(line.getStatut(), search.getText()));
        register(refresh);
        search.textProperty().addListener((obs, old, value) -> refresh.run());

        Button mark = action("Enregistrer presence", () -> {
            List<PresenceLine> selectedRows = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedRows.isEmpty()) return;
            List<Presence> oldPresences = selectedRows.stream().map(PresenceLine::getPresence).filter(Objects::nonNull).toList();
            List<Presence> createdPresences = new ArrayList<>();
            LocalTime arrival = optionalTime(arrivee.getText());
            LocalTime departure = optionalTime(depart.getText());
            for (PresenceLine selected : selectedRows) {
                String finalStatus = automaticPresenceStatus(statut.getValue(), selected.getPlanning(), arrival);
                presenceDao.mark(selected.getPlanningId(), finalStatus, arrival, departure);
                if (selected.getPresence() == null) {
                    Presence saved = presenceDao.findByPlanningId(selected.getPlanningId());
                    if (saved != null) createdPresences.add(saved);
                }
            }
            pushUndo(() -> {
                createdPresences.forEach(presence -> presenceDao.delete(presence.getId()));
                oldPresences.forEach(presenceDao::restore);
            });
            rememberAction(selectedRows.size() == 1 ? "Presence marquee - " + selectedRows.get(0).getAgent() : selectedRows.size() + " presences marquees");
            table.getSelectionModel().clearSelection();
            selectedInfo.setText("Selectionne une affectation pour renseigner la presence.");
            clear(arrivee, depart);
            refreshAll();
        });
        Button absent = action("Marquer absent", () -> {
            List<PresenceLine> selectedRows = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedRows.isEmpty()) return;
            List<Presence> oldPresences = selectedRows.stream().map(PresenceLine::getPresence).filter(Objects::nonNull).toList();
            List<Presence> createdPresences = new ArrayList<>();
            for (PresenceLine selected : selectedRows) {
                presenceDao.mark(selected.getPlanningId(), "Absent", null, null);
                if (selected.getPresence() == null) {
                    Presence saved = presenceDao.findByPlanningId(selected.getPlanningId());
                    if (saved != null) createdPresences.add(saved);
                }
            }
            pushUndo(() -> {
                createdPresences.forEach(presence -> presenceDao.delete(presence.getId()));
                oldPresences.forEach(presenceDao::restore);
            });
            rememberAction(selectedRows.size() == 1 ? "Absence marquee - " + selectedRows.get(0).getAgent() : selectedRows.size() + " absences marquees");
            table.getSelectionModel().clearSelection();
            selectedInfo.setText("Selectionne une affectation pour renseigner la presence.");
            clear(arrivee, depart);
            refreshAll();
        });
        Button deletePresence = danger("Supprimer presence", () -> {
            List<PresenceLine> selectedRows = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedRows.isEmpty()) return;
            List<Presence> oldPresences = selectedRows.stream().map(PresenceLine::getPresence).filter(Objects::nonNull).toList();
            if (oldPresences.isEmpty()) return;
            for (Presence presence : oldPresences) {
                presenceDao.delete(presence.getId());
            }
            pushUndo(() -> oldPresences.forEach(presenceDao::restore));
            rememberAction(oldPresences.size() == 1 ? "Presence supprimee - " + selectedRows.get(0).getAgent() : oldPresences.size() + " presences supprimees");
            table.getSelectionModel().clearSelection();
            selectedInfo.setText("Selectionne une affectation pour renseigner la presence.");
            clear(arrivee, depart);
            refreshAll();
        });

        return panel(new VBox(12, search, selectedInfo, new GridPaneBuilder().add(statut, arrivee, depart).build()), new HBox(10, mark, absent, deletePresence), table);
    }

    private javafx.scene.Node incidentsView() {
        TableView<Incident> table = table();
        table.getColumns().addAll(
                column("ID", "id", 60), column("Site", "site", 200), column("Agent", "agent", 200),
                column("Description", "description", 430), column("Gravite", "gravite", 120), column("Date", "dateIncident", 180)
        );

        ComboBox<Site> site = new ComboBox<>();
        ComboBox<Agent> agent = new ComboBox<>();
        TextArea description = new TextArea();
        description.setPromptText("Description de l'incident");
        description.setPrefRowCount(3);
        ComboBox<String> gravite = combo("Faible", "Moyenne", "Elevee", "Critique");
        colorCombo(gravite, this::severityColor);
        TextField search = field("Rechercher un incident...");
        table.setRowFactory(view -> new TableRow<>() {
            @Override
            protected void updateItem(Incident incident, boolean empty) {
                super.updateItem(incident, empty);
                if (empty || incident == null) setStyle("");
                else if ("Critique".equalsIgnoreCase(incident.getGravite())) setStyle("-fx-background-color:#7f1d1d;-fx-text-background-color:white;");
                else if ("Elevee".equalsIgnoreCase(incident.getGravite())) setStyle("-fx-background-color:#fecaca;-fx-text-background-color:#b91c1c;");
                else if ("Moyenne".equalsIgnoreCase(incident.getGravite())) setStyle("-fx-background-color:#fef3c7;-fx-text-background-color:#c2410c;");
                else setStyle("-fx-background-color:#eef2ff;-fx-text-background-color:#1d4ed8;");
            }
        });

        Runnable refresh = () -> {
            site.setItems(FXCollections.observableArrayList(siteDao.findAll()));
            agent.setItems(FXCollections.observableArrayList(agentDao.findAll()));
            setFilteredItems(table, search, incidentDao.findAll(), incident ->
                    contains(incident.getSite(), search.getText()) || contains(incident.getAgent(), search.getText()) ||
                            contains(incident.getDescription(), search.getText()) || contains(incident.getGravite(), search.getText()));
        };
        register(refresh);
        search.textProperty().addListener((obs, old, value) -> refresh.run());

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                site.setValue(findSite(site.getItems(), selected.getSiteId()));
                agent.setValue(findAgent(agent.getItems(), selected.getAgentId()));
                description.setText(selected.getDescription());
                gravite.setValue(selected.getGravite());
            }
        });

        Button add = action("Declarer incident", () -> {
            if (site.getValue() == null || agent.getValue() == null || description.getText().isBlank()) return;
            incidentDao.save(site.getValue().getId(), agent.getValue().getId(), description.getText(), gravite.getValue());
            List<Incident> allIncidents = incidentDao.findAll();
            if (!allIncidents.isEmpty()) {
                Incident saved = allIncidents.get(0);
                pushUndo(() -> incidentDao.delete(saved.getId()));
            }
            rememberAction("Incident " + gravite.getValue().toLowerCase() + " declare");
            site.getSelectionModel().clearSelection();
            agent.getSelectionModel().clearSelection();
            description.clear();
            refreshAll();
        });
        Button edit = action("Modifier incident", () -> {
            Incident selected = table.getSelectionModel().getSelectedItem();
            if (selected == null || site.getValue() == null || agent.getValue() == null || description.getText().isBlank()) return;
            incidentDao.update(selected.getId(), site.getValue().getId(), agent.getValue().getId(), description.getText(), gravite.getValue());
            pushUndo(() -> incidentDao.update(selected.getId(), selected.getSiteId(), selected.getAgentId(), selected.getDescription(), selected.getGravite()));
            rememberAction("Incident modifie");
            site.getSelectionModel().clearSelection();
            agent.getSelectionModel().clearSelection();
            description.clear();
            table.getSelectionModel().clearSelection();
            refreshAll();
        });
        Button delete = danger("Supprimer incident", () -> {
            Incident selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            incidentDao.delete(selected.getId());
            pushUndo(() -> incidentDao.restore(selected));
            rememberAction("Incident supprime");
            table.getSelectionModel().clearSelection();
            site.getSelectionModel().clearSelection();
            agent.getSelectionModel().clearSelection();
            description.clear();
            refreshAll();
        });

        return panel(new VBox(12, search, new GridPaneBuilder().add(site, agent, description, gravite).build()), new HBox(10, add, edit, delete), table);
    }

    private javafx.scene.Node monthlyPlanningView() {
        DatePicker month = new DatePicker(LocalDate.now().withDayOfMonth(1));
        configureDatePicker(month);
        ListView<Agent> agents = new ListView<>();
        agents.setPrefWidth(280);
        TableView<Planning> table = planningTable();
        TextField search = field("Rechercher dans le planning mensuel...");

        Runnable load = () -> {
            if (month.getValue() != null) {
                agents.setItems(FXCollections.observableArrayList(agentDao.findAll()));
                LocalDate value = month.getValue();
                List<Planning> rows;
                if (agents.getSelectionModel().getSelectedItem() == null) {
                    rows = planningDao.findByMonth(value.getYear(), value.getMonthValue());
                } else {
                    Agent selected = agents.getSelectionModel().getSelectedItem();
                    rows = planningDao.findByAgentAndMonth(selected.getId(), value.getYear(), value.getMonthValue());
                }
                setFilteredItems(table, search, rows, planning ->
                        contains(planning.getAgent(), search.getText()) || contains(planning.getSite(), search.getText()) ||
                                contains(planning.getTypeService(), search.getText()) || contains(String.valueOf(planning.getDateService()), search.getText()));
            }
        };
        month.setOnAction(event -> load.run());
        search.textProperty().addListener((obs, old, value) -> load.run());
        agents.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> load.run());
        register(load);

        Button export = action("Telecharger le planning PDF", () -> exportPlanningPdf(month.getValue(), agents.getSelectionModel().getSelectedItem(), table.getItems()));
        Button allAgents = action("Voir tous les agents", () -> {
            agents.getSelectionModel().clearSelection();
            load.run();
        });
        Label help = new Label("Choisis un mois, puis clique sur un agent pour afficher son planning mensuel. Sans agent selectionne : planning global.");
        help.getStyleClass().add("subtitle");
        HBox content = new HBox(16, agents, table);
        HBox.setHgrow(table, Priority.ALWAYS);
        return panel(new VBox(10, help, search, new GridPaneBuilder().add(month).build()), new HBox(10, allAgents, export), content);
    }

    private TableView<Planning> planningTable() {
        TableView<Planning> table = table();
        table.getColumns().addAll(
                column("ID", "id", 60), column("Agent", "agent", 220), column("Site", "site", 220),
                column("Adresse site", "siteAdresse", 280),
                column("Date", "dateService", 130), column("Debut", "heureDebut", 100),
                column("Fin", "heureFin", 100), column("Type", "typeService", 100)
        );
        table.setRowFactory(view -> new TableRow<>() {
            @Override
            protected void updateItem(Planning planning, boolean empty) {
                super.updateItem(planning, empty);
                if (empty || planning == null) {
                    setStyle("");
                    return;
                }
                String base = siteColor(planning.getSiteId(), 0.18);
                String siteText = siteTextColor(planning.getSiteId());
                if ("Nuit".equalsIgnoreCase(planning.getTypeService())) {
                    setStyle("-fx-background-color:#dbeafe;-fx-text-background-color:" + siteText + ";");
                } else {
                    setStyle("-fx-background-color:" + base + ";-fx-text-background-color:" + siteText + ";");
                }
            }
        });
        return table;
    }

    private void exportPlanningPdf(LocalDate month, Agent agent, List<Planning> plannings) {
        if (month == null || plannings == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le planning mensuel");
        String target = agent == null ? "global" : agent.getPrenom() + "-" + agent.getNom();
        chooser.setInitialFileName("planning-" + target + "-" + month.getYear() + "-" + month.getMonthValue() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        try {
            writeSimplePdf(file.toPath(), "Planning mensuel " + month.getMonthValue() + "/" + month.getYear(), plannings);
            rememberAction("Planning PDF exporte");
            refreshAll();
        } catch (IOException exception) {
            throw new RuntimeException("Export impossible : " + exception.getMessage(), exception);
        }
    }

    private VBox panel(javafx.scene.Node form, javafx.scene.Node actions, javafx.scene.Node table) {
        VBox box = new VBox(14, form, actions, table);
        box.getStyleClass().add("panel");
        box.setPadding(new Insets(18));
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private <T> TableView<T> table() {
        TableView<T> table = new TableView<>();
        table.setPrefHeight(500);
        return table;
    }

    private <S, T> TableColumn<S, T> column(String title, String property, int width) {
        TableColumn<S, T> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        return column;
    }

    private TextField field(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        return field;
    }

    private ComboBox<String> combo(String... values) {
        ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(values));
        combo.getSelectionModel().selectFirst();
        return combo;
    }

    private void colorCombo(ComboBox<String> combo, Function<String, String> colorProvider) {
        combo.setCellFactory(list -> coloredListCell(colorProvider));
        combo.setButtonCell(coloredListCell(colorProvider));
    }

    private ListCell<String> coloredListCell(Function<String, String> colorProvider) {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                setStyle("-fx-text-fill:" + colorProvider.apply(item) + ";-fx-font-weight:800;");
            }
        };
    }

    private Button action(String label, Runnable runnable) {
        Button button = new Button(label);
        button.getStyleClass().add("primary-button");
        button.setOnAction(event -> safe(runnable));
        return button;
    }

    private Button danger(String label, Runnable runnable) {
        Button button = new Button(label);
        button.getStyleClass().add("danger-button");
        button.setOnAction(event -> safe(runnable));
        return button;
    }

    private void safe(Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Operation impossible");
            alert.setContentText(exception.getMessage());
            alert.showAndWait();
        }
    }

    private void pushUndo(Runnable undo) {
        undoStack.addLast(undo);
        undoButton.setDisable(false);
    }

    private void register(Runnable refresh) {
        refreshers.add(refresh);
    }

    private void refreshAll() {
        for (Runnable refresh : refreshers) {
            refresh.run();
        }
    }

    private Scene scene(javafx.scene.Parent root, int width, int height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        return scene;
    }

    private void clear(TextField... fields) {
        for (TextField field : fields) {
            field.clear();
        }
    }

    private void clearAgentForm(TableView<Agent> table, TextField nom, TextField prenom, TextField telephone, TextField email, TextField poste, TextField documentPath, ComboBox<String> statut, ComboBox<String> sexe, ComboBox<String> typeContrat, DatePicker embauche, DatePicker naissance) {
        table.getSelectionModel().clearSelection();
        clear(nom, prenom, telephone, email, poste, documentPath);
        statut.getSelectionModel().selectFirst();
        sexe.getSelectionModel().selectFirst();
        typeContrat.getSelectionModel().selectFirst();
        embauche.setValue(LocalDate.now());
        naissance.setValue(null);
    }

    private void clearSiteForm(TableView<Site> table, TextField nom, TextField adresse, TextField client, TextField agentsJour, TextField agentsNuit, DatePicker dateBesoin) {
        table.getSelectionModel().clearSelection();
        clear(nom, adresse, client, agentsJour, agentsNuit);
        dateBesoin.setValue(LocalDate.now());
    }

    private int integer(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception exception) {
            return fallback;
        }
    }

    private LocalTime time(String value) {
        return LocalTime.parse(value.trim());
    }

    private LocalTime optionalTime(String value) {
        return value == null || value.isBlank() ? null : time(value);
    }

    private Agent findAgent(List<Agent> agents, int id) {
        for (Agent agent : agents) {
            if (agent.getId() == id) {
                return agent;
            }
        }
        return null;
    }

    private Site findSite(List<Site> sites, int id) {
        for (Site site : sites) {
            if (site.getId() == id) {
                return site;
            }
        }
        return null;
    }

    private Planning findPlanning(List<Planning> plannings, int id) {
        for (Planning planning : plannings) {
            if (planning.getId() == id) return planning;
        }
        return null;
    }

    private String automaticPresenceStatus(String selectedStatus, Planning planning, LocalTime arrival) {
        if ("Absent".equalsIgnoreCase(selectedStatus) || "Retard".equalsIgnoreCase(selectedStatus)) {
            return selectedStatus;
        }
        if (planning != null && arrival != null && "Present".equalsIgnoreCase(selectedStatus) && isLateArrival(planning, arrival)) {
            return "Retard";
        }
        return selectedStatus;
    }

    private boolean isLateArrival(Planning planning, LocalTime arrival) {
        LocalTime start = planning.getHeureDebut();
        LocalTime end = planning.getHeureFin();
        if (end.isAfter(start)) {
            return arrival.isAfter(start);
        }
        return arrival.isAfter(start) || arrival.isBefore(end);
    }

    private Label statCard(String title, String value, String background, String color) {
        Label label = new Label(title + "\n" + value);
        label.setMinWidth(190);
        label.setMinHeight(92);
        label.setPadding(new Insets(16));
        label.setStyle("-fx-background-color:" + background + ";-fx-text-fill:" + color + ";-fx-font-size:18px;-fx-font-weight:800;-fx-background-radius:16;");
        return label;
    }

    private <T> void setFilteredItems(TableView<T> table, TextField search, List<T> values, Predicate<T> predicate) {
        String query = search == null ? "" : search.getText();
        if (query == null || query.isBlank()) {
            table.setItems(FXCollections.observableArrayList(values));
            return;
        }
        table.setItems(FXCollections.observableArrayList(values.stream().filter(predicate).toList()));
    }

    private boolean contains(String value, String query) {
        return value != null && query != null && value.toLowerCase().contains(query.toLowerCase());
    }

    private Label titleLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private Label alertLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("alert-text");
        return label;
    }

    private VBox chartCard(javafx.scene.chart.Chart chart) {
        VBox box = new VBox(chart);
        box.getStyleClass().add("chart-card");
        box.setMinWidth(520);
        VBox.setVgrow(chart, Priority.ALWAYS);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private GridPane dashboardGrid(String... headers) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("dashboard-grid");
        grid.setHgap(1);
        grid.setVgap(1);
        for (int i = 0; i < headers.length; i++) {
            Label label = new Label(headers[i]);
            label.getStyleClass().add("dashboard-cell-header");
            label.setMinWidth(i == 0 ? 190 : 110);
            grid.add(label, i, 0);
        }
        return grid;
    }

    private void fillSitesTodayTable(GridPane grid, List<Site> sites, List<Planning> rows) {
        grid.getChildren().removeIf(node -> GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) > 0);
        int row = 1;
        for (Site site : sites) {
            List<Planning> siteRows = rowsForSiteDate(site, rows);
            long jour = siteRows.stream().filter(p -> "Jour".equalsIgnoreCase(p.getTypeService())).count();
            long nuit = siteRows.stream().filter(p -> "Nuit".equalsIgnoreCase(p.getTypeService())).count();
            boolean ok = jour == site.getAgentsJour() && nuit == site.getAgentsNuit();
            addDashboardRow(grid, row++, site.getNom(), dashboardSiteDate(site, rows), jour + "/" + site.getAgentsJour(), nuit + "/" + site.getAgentsNuit(), ok ? "OK" : "Attention");
        }
    }

    private boolean isSiteExactlyCovered(Site site, List<Planning> rows) {
        long jour = rows.stream().filter(p -> "Jour".equalsIgnoreCase(p.getTypeService())).count();
        long nuit = rows.stream().filter(p -> "Nuit".equalsIgnoreCase(p.getTypeService())).count();
        return jour == site.getAgentsJour() && nuit == site.getAgentsNuit();
    }

    private List<Site> sitesForDashboard(List<Site> sites, List<Planning> monthRows, YearMonth month) {
        return sites.stream()
                .filter(site -> site.getDateBesoin() == null
                        ? monthRows.stream().anyMatch(row -> row.getSiteId() == site.getId())
                        : YearMonth.from(site.getDateBesoin()).equals(month))
                .toList();
    }

    private List<Planning> rowsForSiteDate(Site site, List<Planning> rows) {
        return rows.stream()
                .filter(row -> row.getSiteId() == site.getId())
                .filter(row -> site.getDateBesoin() == null || row.getDateService().equals(site.getDateBesoin()))
                .toList();
    }

    private String dashboardSiteDate(Site site, List<Planning> rows) {
        if (site.getDateBesoin() != null) return String.valueOf(site.getDateBesoin());
        return rows.stream()
                .filter(row -> row.getSiteId() == site.getId())
                .map(row -> String.valueOf(row.getDateService()))
                .findFirst()
                .orElse("-");
    }

    private void fillHistoryTable(GridPane grid) {
        grid.getChildren().removeIf(node -> GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) > 0);
        int row = 1;
        if (actionHistory.isEmpty()) {
            addDashboardRow(grid, row, "--", "Aucune action recente");
            return;
        }
        for (String entry : actionHistory.stream().limit(6).toList()) {
            String[] parts = entry.split("\\|", 2);
            addDashboardRow(grid, row++, parts[0], parts.length > 1 ? parts[1] : "");
        }
    }

    private void addDashboardRow(GridPane grid, int row, String... values) {
        for (int i = 0; i < values.length; i++) {
            Label label = new Label(values[i]);
            label.getStyleClass().add("dashboard-cell");
            label.setMinWidth(i == 0 ? 190 : 110);
            grid.add(label, i, row);
        }
    }

    private void rememberAction(String action) {
        actionHistory.addFirst(LocalTime.now().withSecond(0).withNano(0) + "|" + action);
        while (actionHistory.size() > 12) actionHistory.removeLast();
    }

    private List<String> dashboardAlerts(LocalDate today, List<Site> sites, List<Planning> rows, List<Presence> monthPresences, List<Incident> todayIncidents, Map<Integer, Planning> planningById) {
        List<String> alerts = new ArrayList<>();
        for (Presence presence : monthPresences) {
            Planning planning = planningById.get(presence.getPlanningId());
            if (planning != null && planning.getDateService().equals(today) && "Absent".equalsIgnoreCase(presence.getStatut())) {
                alerts.add("1 agent absent au " + planning.getSite());
            }
        }
        for (Site site : sites) {
            List<Planning> siteRows = rowsForSiteDate(site, rows);
            long jour = siteRows.stream().filter(p -> "Jour".equalsIgnoreCase(p.getTypeService())).count();
            long nuit = siteRows.stream().filter(p -> "Nuit".equalsIgnoreCase(p.getTypeService())).count();
            if (jour < site.getAgentsJour() || nuit < site.getAgentsNuit()) {
                alerts.add("1 planning incomplet pour " + site.getNom());
            } else if (jour > site.getAgentsJour() || nuit > site.getAgentsNuit()) {
                alerts.add("1 planning en sureffectif pour " + site.getNom());
            }
        }
        long severe = todayIncidents.stream().filter(i -> "Elevee".equalsIgnoreCase(i.getGravite()) || "Critique".equalsIgnoreCase(i.getGravite())).count();
        if (severe > 0) alerts.add(severe + " incident grave declare");
        return alerts.stream().limit(5).toList();
    }

    private List<Agent> choosePlanningAgents(List<Agent> currentSelection) {
        Dialog<List<Agent>> dialog = new Dialog<>();
        dialog.setTitle("Selection des agents");
        dialog.setHeaderText("Coche les agents a affecter au planning");
        ButtonType validate = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(validate, ButtonType.CANCEL);

        TilePane grid = new TilePane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPrefColumns(4);
        List<CheckBox> checks = new ArrayList<>();
        List<Integer> currentIds = currentSelection.stream().map(Agent::getId).toList();
        for (Agent agent : agentDao.findAll()) {
            CheckBox check = new CheckBox(agent.toString());
            check.setUserData(agent);
            check.setSelected(currentIds.contains(agent.getId()));
            check.getStyleClass().add("agent-check");
            checks.add(check);
            grid.getChildren().add(check);
        }
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == validate ? checkedAgents(checks) : null);
        return dialog.showAndWait().orElse(null);
    }

    private void applyDefaultPlanningHours(String type, TextField debut, TextField fin) {
        if ("Nuit".equalsIgnoreCase(type)) {
            debut.setText("20:00");
            fin.setText("08:00");
        } else {
            debut.setText("08:00");
            fin.setText("20:00");
        }
    }

    private void configureDatePicker(DatePicker picker) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        picker.setPromptText("jj/mm/aaaa");
        picker.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : formatter.format(date);
            }

            @Override
            public LocalDate fromString(String value) {
                if (value == null || value.isBlank()) return null;
                try {
                    return LocalDate.parse(value.trim(), formatter);
                } catch (DateTimeParseException exception) {
                    return LocalDate.parse(value.trim());
                }
            }
        });
    }

    private boolean validateAgentForm(Integer ignoredId, String nom, String prenom, String telephone, String email, LocalDate naissance) {
        if (nom == null || nom.isBlank() || prenom == null || prenom.isBlank()) {
            warning("Agent incomplet", "Le nom et le prenom sont obligatoires.");
            return false;
        }
        if (naissance == null) {
            warning("Date de naissance obligatoire", "Renseigne la date de naissance de l'agent.");
            return false;
        }
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            warning("Email invalide", "L'adresse mail doit respecter un format valide, par exemple agent@email.fr.");
            return false;
        }
        if (telephone == null || !telephone.matches("^(\\+33|0)[1-9](\\d{2}){4}$")) {
            warning("Telephone invalide", "Le telephone doit respecter un format francais, par exemple 0601020304 ou +33601020304.");
            return false;
        }
        for (Agent agent : agentDao.findAll()) {
            if (ignoredId != null && agent.getId() == ignoredId) continue;
            if (agent.getNom().equalsIgnoreCase(nom.trim()) &&
                    agent.getPrenom().equalsIgnoreCase(prenom.trim()) &&
                    naissance.equals(agent.getDateNaissance())) {
                warning("Agent deja existant", "Un agent avec le meme nom, prenom et date de naissance existe deja.");
                return false;
            }
        }
        return true;
    }

    private String fileName(String path) {
        if (path == null || path.isBlank()) return "";
        return Path.of(path).getFileName().toString();
    }

    private String siteColor(int siteId, double alpha) {
        String[] colors = {"rgba(219,234,254,", "rgba(220,252,231,", "rgba(254,249,195,", "rgba(237,233,254,", "rgba(255,237,213,"};
        return colors[Math.abs(siteId) % colors.length] + alpha + ")";
    }

    private String siteTextColor(int siteId) {
        String[] colors = {"#1d4ed8", "#15803d", "#a16207", "#7c3aed", "#c2410c", "#0f766e"};
        return colors[Math.abs(siteId) % colors.length];
    }

    private String statusColor(String status) {
        if ("Actif".equalsIgnoreCase(status)) return "#15803d";
        if ("Inactif".equalsIgnoreCase(status)) return "#b91c1c";
        if ("Conge".equalsIgnoreCase(status)) return "#c2410c";
        return "#1f2a44";
    }

    private String contractColor(String contract) {
        if ("CDI".equalsIgnoreCase(contract)) return "#15803d";
        if ("CDD".equalsIgnoreCase(contract)) return "#c2410c";
        if ("Interim".equalsIgnoreCase(contract)) return "#1d4ed8";
        if ("Stage".equalsIgnoreCase(contract)) return "#7c3aed";
        return "#1f2a44";
    }

    private String presenceColor(String status) {
        if ("Present".equalsIgnoreCase(status)) return "#15803d";
        if ("Absent".equalsIgnoreCase(status)) return "#b91c1c";
        if ("Retard".equalsIgnoreCase(status)) return "#c2410c";
        return "#1f2a44";
    }

    private String severityColor(String severity) {
        if ("Critique".equalsIgnoreCase(severity)) return "#7f1d1d";
        if ("Elevee".equalsIgnoreCase(severity)) return "#b91c1c";
        if ("Moyenne".equalsIgnoreCase(severity)) return "#c2410c";
        if ("Faible".equalsIgnoreCase(severity)) return "#1d4ed8";
        return "#1f2a44";
    }

    private String storeDocument(String source) {
        if (source == null || source.isBlank()) return "";
        try {
            Path sourcePath = Path.of(source);
            if (!Files.exists(sourcePath)) return source;
            if (!isPdf(sourcePath)) {
                throw new RuntimeException("Le document agent doit etre un fichier PDF.");
            }
            Path docsDir = Path.of("documents-agents");
            Files.createDirectories(docsDir);
            String fileName = sourcePath.getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
            Path target = docsDir.resolve(System.currentTimeMillis() + "-" + fileName);
            Files.copy(sourcePath, target, StandardCopyOption.REPLACE_EXISTING);
            return target.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new RuntimeException("Document impossible a stocker : " + exception.getMessage(), exception);
        }
    }

    private boolean isPdf(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".pdf");
    }

    private void openDocument(String path) {
        if (path == null || path.isBlank()) return;
        try {
            File file = new File(path);
            if (file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (IOException exception) {
            throw new RuntimeException("Ouverture document impossible : " + exception.getMessage(), exception);
        }
    }

    private List<Agent> checkedAgents(List<CheckBox> checks) {
        return checks.stream()
                .filter(CheckBox::isSelected)
                .map(check -> (Agent) check.getUserData())
                .toList();
    }

    private void clearAgentChecks(List<CheckBox> checks) {
        checks.forEach(check -> check.setSelected(false));
    }

    private boolean validatePlanningSelection(Site site, List<Agent> agents, LocalDate date, LocalTime start, LocalTime end, String type, Integer ignoredPlanningId, boolean validateCount) {
        if (site == null || date == null || start == null || end == null || type == null || agents.isEmpty()) {
            warning("Planning incomplet", "Selectionne un site, une date, les horaires, le type et au moins un agent.");
            return false;
        }
        int required = requiredAgents(site, type);
        if (site.getDateBesoin() != null && !site.getDateBesoin().equals(date)) {
            warning("Date non conforme", "Le besoin du site \"" + site.getNom() + "\" est prevu pour le " + site.getDateBesoin() + ".\nDate selectionnee : " + date + ".");
            return false;
        }
        if (validateCount && agents.size() != required) {
            String sense = agents.size() < required ? "pas assez" : "trop";
            warning("Planning incomplet",
                    "Le site \"" + site.getNom() + "\" a besoin de " + required +
                            " agent(s) en " + type + " pour le " + date + ".\nTu as selectionne " + agents.size() +
                            " agent(s), c'est " + sense + ".");
            return false;
        }
        for (Agent agent : agents) {
            String conflict = planningConflictMessage(agent, date, start, end, type, ignoredPlanningId);
            if (conflict != null) {
                warning("Agent deja affecte", conflict);
                return false;
            }
        }
        return true;
    }

    private int requiredAgents(Site site, String type) {
        if ("Nuit".equalsIgnoreCase(type)) {
            return site.getAgentsNuit();
        }
        return site.getAgentsJour();
    }

    private String planningConflictMessage(Agent agent, LocalDate date, LocalTime start, LocalTime end, String type, Integer ignoredPlanningId) {
        for (Planning row : planningDao.findAll()) {
            if (ignoredPlanningId != null && row.getId() == ignoredPlanningId) continue;
            if (row.getAgentId() != agent.getId() || !row.getDateService().equals(date)) continue;
            if (!row.getTypeService().equalsIgnoreCase(type)) {
                return agent + " est deja affecte en " + row.getTypeService() + " le " + date + ". Impossible de l'affecter aussi en " + type + ".";
            }
            if (overlaps(start, end, row.getHeureDebut(), row.getHeureFin())) {
                return agent + " est deja affecte le " + date + " de " + row.getHeureDebut() + " a " + row.getHeureFin() + ".";
            }
        }
        return null;
    }

    private boolean overlaps(LocalTime start, LocalTime end, LocalTime existingStart, LocalTime existingEnd) {
        int s1 = start.toSecondOfDay();
        int e1 = end.toSecondOfDay();
        int s2 = existingStart.toSecondOfDay();
        int e2 = existingEnd.toSecondOfDay();
        if (e1 <= s1) e1 += 24 * 3600;
        if (e2 <= s2) e2 += 24 * 3600;
        return s1 < e2 && e1 > s2;
    }

    private void warning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showIncompletePlanningAlerts(LocalDate date) {
        if (date == null) return;
        List<Planning> plannings = planningDao.findAll().stream().filter(p -> p.getDateService().equals(date)).toList();
        for (Site site : siteDao.findAll()) {
            long assigned = plannings.stream().filter(p -> p.getSiteId() == site.getId()).count();
            if (assigned > 0 && assigned < site.getAgentsNecessaires()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Planning incomplet");
                alert.setHeaderText("⚠ Planning incomplet");
                alert.setContentText("Le site \"" + site.getNom() + "\"\nn'a pas assez d'agents affectes\npour le " + date + ".\n\nNecessaires : " + site.getAgentsNecessaires() + "\nAffectes : " + assigned);
                alert.showAndWait();
            }
        }
    }

    private void writeSimplePdf(Path file, String title, List<Planning> rows) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDRectangle landscapeA4 = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
            PDPage page = new PDPage(landscapeA4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                writePdfText(content, title, 40, 545, 16, true);
                float y = 490;
                float[] widths = {105, 140, 230, 85, 60, 60, 65};
                String[] headers = {"Agent", "Site", "Adresse", "Date", "Debut", "Fin", "Type"};
                drawPdfRow(content, headers, widths, 40, y, true);
                y -= 24;
                if (rows.isEmpty()) {
                    writePdfText(content, "Aucune affectation pour cette selection.", 40, y, 11, false);
                } else {
                    for (Planning row : rows) {
                        if (y < 45) {
                            break;
                        }
                        drawPdfRow(content, new String[]{
                                cleanPdfText(row.getAgent()),
                                cleanPdfText(row.getSite()),
                                cleanPdfText(row.getSiteAdresse()),
                                String.valueOf(row.getDateService()),
                                String.valueOf(row.getHeureDebut()),
                                String.valueOf(row.getHeureFin()),
                                cleanPdfText(row.getTypeService())
                        }, widths, 40, y, false);
                        y -= 24;
                    }
                }
            }
            document.save(file.toFile());
        }
    }

    private void drawPdfRow(PDPageContentStream content, String[] cells, float[] widths, float x, float y, boolean header) throws IOException {
        float height = 22;
        float currentX = x;
        content.setNonStrokingColor(header ? java.awt.Color.decode("#dbeafe") : java.awt.Color.WHITE);
        content.addRect(x, y - height + 5, sum(widths), height);
        content.fill();
        content.setStrokingColor(java.awt.Color.decode("#94a3b8"));
        currentX = x;
        for (float width : widths) {
            content.addRect(currentX, y - height + 5, width, height);
            currentX += width;
        }
        content.stroke();
        currentX = x + 4;
        for (int i = 0; i < cells.length; i++) {
            writePdfText(content, cells[i], currentX, y - 10, header ? 9 : 8, header);
            currentX += widths[i];
        }
    }

    private float sum(float[] values) {
        float total = 0;
        for (float value : values) total += value;
        return total;
    }

    private String truncate(String value, float width) {
        int max = Math.max(8, (int) (width / 6));
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }

    private void writePdfText(PDPageContentStream content, String text, float x, float y, int size, boolean bold) throws IOException {
        content.beginText();
        content.setNonStrokingColor(java.awt.Color.BLACK);
        content.setFont(bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, size);
        content.newLineAtOffset(x, y);
        content.showText(cleanPdfText(text));
        content.endText();
    }

    private String cleanPdfText(String value) {
        return value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", "");
    }

    private TableColumn<PresenceLine, String> presenceColumn(String title, Function<PresenceLine, String> extractor, int width) {
        TableColumn<PresenceLine, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(extractor.apply(data.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private List<PresenceLine> buildPresenceLines() {
        Map<Integer, Presence> presenceByPlanning = new HashMap<>();
        for (Presence presence : presenceDao.findAll()) {
            presenceByPlanning.put(presence.getPlanningId(), presence);
        }
        return planningDao.findAll().stream()
                .map(planning -> new PresenceLine(planning, presenceByPlanning.get(planning.getId())))
                .sorted(Comparator.comparing(PresenceLine::getDate).reversed().thenComparing(PresenceLine::getAgent))
                .toList();
    }

    private static class PresenceLine {
        private final Planning planning;
        private final Presence presence;

        PresenceLine(Planning planning, Presence presence) {
            this.planning = planning;
            this.presence = presence;
        }

        Planning getPlanning() { return planning; }
        Presence getPresence() { return presence; }
        int getPlanningId() { return planning.getId(); }
        String getPlanningIdLabel() { return "#" + planning.getId(); }
        String getDate() { return String.valueOf(planning.getDateService()); }
        String getAgent() { return planning.getAgent(); }
        String getSite() { return planning.getSite(); }
        String getSiteAdresse() { return planning.getSiteAdresse(); }
        String getType() { return planning.getTypeService(); }
        String getPlannedHours() { return planning.getHeureDebut() + " - " + planning.getHeureFin(); }
        String getStatut() { return presence == null ? "A renseigner" : presence.getStatut(); }
        String getArrivee() { return presence == null || presence.getHeureArrivee() == null ? "" : presence.getHeureArrivee().toString(); }
        String getDepart() { return presence == null || presence.getHeureDepart() == null ? "" : presence.getHeureDepart().toString(); }
    }

    private static class GridPaneBuilder {
        private final GridPane grid = new GridPane();
        private int index = 0;

        GridPaneBuilder() {
            grid.setHgap(12);
            grid.setVgap(12);
        }

        GridPaneBuilder add(javafx.scene.Node... nodes) {
            for (javafx.scene.Node node : nodes) {
                int col = index % 3;
                int row = index / 3;
                node.setStyle("-fx-min-width: 250;");
                grid.add(node, col, row);
                index++;
            }
            return this;
        }

        GridPane build() {
            return grid;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
