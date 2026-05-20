CREATE DATABASE IF NOT EXISTS securestaff_desktop
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE securestaff_desktop;

CREATE TABLE IF NOT EXISTS admins (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(80) NOT NULL UNIQUE,
  password VARCHAR(120) NOT NULL
);

CREATE TABLE IF NOT EXISTS agents (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nom VARCHAR(100) NOT NULL,
  prenom VARCHAR(100) NOT NULL,
  telephone VARCHAR(30),
  email VARCHAR(150),
  poste VARCHAR(100),
  statut VARCHAR(40) NOT NULL,
  date_embauche DATE,
  date_naissance DATE,
  sexe VARCHAR(20),
  document_path VARCHAR(500),
  type_contrat VARCHAR(40)
);

CREATE TABLE IF NOT EXISTS sites (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nom VARCHAR(120) NOT NULL,
  adresse VARCHAR(255) NOT NULL,
  client VARCHAR(120) NOT NULL,
  agents_necessaires INT NOT NULL DEFAULT 1,
  agents_jour INT NOT NULL DEFAULT 1,
  agents_nuit INT NOT NULL DEFAULT 0,
  date_besoin DATE
);

CREATE TABLE IF NOT EXISTS plannings (
  id INT AUTO_INCREMENT PRIMARY KEY,
  agent_id INT NOT NULL,
  site_id INT NOT NULL,
  date_service DATE NOT NULL,
  heure_debut TIME NOT NULL,
  heure_fin TIME NOT NULL,
  type_service VARCHAR(20) NOT NULL,
  FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE,
  FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS presences (
  id INT AUTO_INCREMENT PRIMARY KEY,
  planning_id INT NOT NULL,
  statut VARCHAR(30) NOT NULL,
  heure_arrivee TIME,
  heure_depart TIME,
  UNIQUE KEY unique_planning_presence (planning_id),
  FOREIGN KEY (planning_id) REFERENCES plannings(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS incidents (
  id INT AUTO_INCREMENT PRIMARY KEY,
  site_id INT NOT NULL,
  agent_id INT NOT NULL,
  description TEXT NOT NULL,
  gravite VARCHAR(30) NOT NULL,
  date_incident DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE CASCADE,
  FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS app_meta (
  meta_key VARCHAR(80) PRIMARY KEY,
  meta_value VARCHAR(120) NOT NULL
);

DELETE FROM incidents;
DELETE FROM presences;
DELETE FROM plannings;
DELETE FROM sites;
DELETE FROM agents;
DELETE FROM admins;
DELETE FROM app_meta WHERE meta_key = 'seed_version';

ALTER TABLE incidents AUTO_INCREMENT = 1;
ALTER TABLE presences AUTO_INCREMENT = 1;
ALTER TABLE plannings AUTO_INCREMENT = 1;
ALTER TABLE sites AUTO_INCREMENT = 1;
ALTER TABLE agents AUTO_INCREMENT = 1;
ALTER TABLE admins AUTO_INCREMENT = 1;

INSERT INTO admins (username, password) VALUES ('admin', 'admin123');

INSERT INTO agents (nom, prenom, telephone, email, poste, statut, date_embauche, date_naissance, sexe, type_contrat) VALUES
('Martin', 'Clara', '0612457810', 'clara.martin@securestaff.fr', 'Cheffe de site', 'Actif', '2026-04-01', '1994-03-12', 'Femme', 'CDI'),
('Ahmed', 'Yanis', '0622334455', 'yanis.ahmed@securestaff.fr', 'Agent de securite', 'Actif', '2026-04-10', '1998-11-02', 'Homme', 'CDD'),
('Durand', 'Leo', '0633445566', 'leo.durand@securestaff.fr', 'Agent SSIAP', 'Actif', '2026-05-01', '1996-06-22', 'Homme', 'CDI'),
('Moreau', 'Ines', '0644556677', 'ines.moreau@securestaff.fr', 'Agent accueil', 'Actif', '2026-05-05', '1999-01-14', 'Femme', 'Interim'),
('Petit', 'Karim', '0655667788', 'karim.petit@securestaff.fr', 'Rondier de nuit', 'Actif', '2026-05-12', '1992-09-09', 'Homme', 'CDI'),
('Bernard', 'Sarah', '0666778899', 'sarah.bernard@securestaff.fr', 'Agent de securite', 'Conge', '2026-03-20', '1995-12-01', 'Femme', 'CDD'),
('Leroy', 'Hugo', '0677889900', 'hugo.leroy@securestaff.fr', 'Agent evenementiel', 'Inactif', '2026-02-15', '1991-04-18', 'Homme', 'Stage');

INSERT INTO sites (nom, adresse, client, agents_necessaires, agents_jour, agents_nuit, date_besoin) VALUES
('Centre Commercial Horizon', '15 Rue Victor Hugo, 75015 Paris', 'Horizon Retail', 4, 2, 2, '2026-05-17'),
('Entrepot Nord Logistic', '8 Avenue des Metiers, 93200 Saint-Denis', 'Nord Logistic', 3, 2, 1, '2026-05-18'),
('Campus IRIS', '10 Rue de la Paix, 75002 Paris', 'Ecole IRIS', 2, 1, 1, '2026-06-03'),
('Galerie Rivoli', '22 Rue de Rivoli, 75004 Paris', 'Galerie Rivoli', 3, 2, 1, '2026-06-12');

INSERT INTO plannings (agent_id, site_id, date_service, heure_debut, heure_fin, type_service) VALUES
(1, 1, '2026-05-17', '08:00:00', '20:00:00', 'Jour'),
(2, 1, '2026-05-17', '08:00:00', '20:00:00', 'Jour'),
(5, 1, '2026-05-17', '20:00:00', '08:00:00', 'Nuit'),
(3, 1, '2026-05-17', '20:00:00', '08:00:00', 'Nuit'),
(3, 2, '2026-05-18', '08:00:00', '20:00:00', 'Jour'),
(4, 2, '2026-05-18', '08:00:00', '20:00:00', 'Jour'),
(5, 2, '2026-05-18', '20:00:00', '08:00:00', 'Nuit'),
(1, 3, '2026-06-03', '08:00:00', '20:00:00', 'Jour'),
(5, 3, '2026-06-03', '20:00:00', '08:00:00', 'Nuit'),
(2, 4, '2026-06-12', '08:00:00', '20:00:00', 'Jour'),
(4, 4, '2026-06-12', '08:00:00', '20:00:00', 'Jour'),
(3, 4, '2026-06-12', '20:00:00', '08:00:00', 'Nuit');

INSERT INTO presences (planning_id, statut, heure_arrivee, heure_depart) VALUES
(1, 'Present', '08:00:00', '20:00:00'),
(2, 'Retard', '08:35:00', '20:00:00'),
(3, 'Absent', NULL, NULL),
(4, 'Present', '20:00:00', '08:00:00'),
(5, 'Present', '08:00:00', '20:00:00'),
(6, 'Retard', '08:20:00', '20:00:00'),
(7, 'Absent', NULL, NULL),
(8, 'Present', '08:00:00', '20:00:00'),
(9, 'Present', '20:00:00', '08:00:00'),
(10, 'Present', '08:00:00', '20:00:00'),
(11, 'Absent', NULL, NULL),
(12, 'Retard', '20:25:00', '08:00:00');

INSERT INTO incidents (site_id, agent_id, description, gravite, date_incident) VALUES
(1, 2, 'Arrivee tardive signalee au poste principal', 'Moyenne', '2026-05-17 08:40:00'),
(2, 5, 'Absence non remplacee sur le creneau nuit', 'Elevee', '2026-05-18 20:15:00'),
(4, 3, 'Alarme intrusion declenchee pendant la ronde', 'Critique', '2026-06-12 22:30:00');

INSERT INTO app_meta (meta_key, meta_value) VALUES ('seed_version', 'securestaff-clean-2026-05-17-v4')
ON DUPLICATE KEY UPDATE meta_value = VALUES(meta_value);
