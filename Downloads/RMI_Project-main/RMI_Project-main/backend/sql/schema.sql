-- ============================================================
--  Schema MySQL/MariaDB — Gestion des Stocks Multi-Entrepôts
--  Plateforme RMI
-- ============================================================

CREATE DATABASE IF NOT EXISTS rmi_stocks
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE rmi_stocks;

-- ── Produits ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS produits (
                                        id            VARCHAR(20)    NOT NULL PRIMARY KEY,
                                        nom           VARCHAR(100)   NOT NULL,
                                        categorie     VARCHAR(50)    NOT NULL,
                                        prix_unitaire DECIMAL(10,2)  NOT NULL CHECK (prix_unitaire >= 0),
                                        cree_le       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ── Entrepôts ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS entrepots (
                                         id      VARCHAR(20)   NOT NULL PRIMARY KEY,
                                         nom     VARCHAR(100)  NOT NULL,
                                         ville   VARCHAR(100)  NOT NULL,
                                         cree_le TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ── Stocks (quantité courante par entrepôt × produit) ────────
CREATE TABLE IF NOT EXISTS stocks (
                                      id_entrepot  VARCHAR(20)  NOT NULL,
                                      id_produit   VARCHAR(20)  NOT NULL,
                                      quantite     INT          NOT NULL DEFAULT 0 CHECK (quantite >= 0),
                                      mis_a_jour   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
                                          ON UPDATE CURRENT_TIMESTAMP,
                                      PRIMARY KEY (id_entrepot, id_produit),
                                      CONSTRAINT fk_stock_entrepot FOREIGN KEY (id_entrepot)
                                          REFERENCES entrepots(id) ON DELETE RESTRICT ON UPDATE CASCADE,
                                      CONSTRAINT fk_stock_produit  FOREIGN KEY (id_produit)
                                          REFERENCES produits(id)  ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ── Mouvements (historique complet, jamais modifié) ──────────
CREATE TABLE IF NOT EXISTS mouvements (
                                          id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                          id_entrepot  VARCHAR(20)   NOT NULL,
                                          id_produit   VARCHAR(20)   NOT NULL,
                                          type_mouv    ENUM('ENTREE','SORTIE','TRANSFERT') NOT NULL,
                                          quantite     INT           NOT NULL CHECK (quantite > 0),
                                          note         VARCHAR(255),
                                          date_heure   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          CONSTRAINT fk_mouv_entrepot FOREIGN KEY (id_entrepot)
                                              REFERENCES entrepots(id) ON DELETE RESTRICT ON UPDATE CASCADE,
                                          CONSTRAINT fk_mouv_produit  FOREIGN KEY (id_produit)
                                              REFERENCES produits(id)  ON DELETE RESTRICT ON UPDATE CASCADE,
                                          INDEX idx_mouv_entrepot (id_entrepot),
                                          INDEX idx_mouv_produit  (id_produit),
                                          INDEX idx_mouv_date     (date_heure)
) ENGINE=InnoDB;

-- ── Création de l'utilisateur applicatif (optionnel) ─────────
-- CREATE USER IF NOT EXISTS 'rmi_user'@'localhost' IDENTIFIED BY 'MotDePasseSecret!';
-- GRANT SELECT, INSERT, UPDATE ON rmi_stocks.* TO 'rmi_user'@'localhost';
-- FLUSH PRIVILEGES;