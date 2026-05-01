-- =====================================================================
-- Migration non-breaking pour intégrer le projet Java à la base Symfony
-- Base : pidev_amine
-- Table cible : `user`
--
-- ⚠️  À exécuter UNE SEULE FOIS dans phpMyAdmin (onglet SQL)
-- ⚠️  Toutes les colonnes sont AJOUTÉES en NULL → Symfony n'est pas cassé
-- =====================================================================

-- Ajoute les colonnes dont le projet Java a besoin et qui n'existent pas
-- encore dans la table `user` Symfony.
ALTER TABLE `user`
    ADD COLUMN IF NOT EXISTS `username`          VARCHAR(50)  NULL DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `session_token`     VARCHAR(255) NULL DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `host_request_date` DATETIME     NULL DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `created_at`        DATETIME     NULL DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `updated_at`        DATETIME     NULL DEFAULT NULL;

-- Index unique sur username (nullable → plusieurs NULL autorisés)
-- Commenté par défaut ; activer si vous voulez forcer l'unicité côté Java.
-- ALTER TABLE `user` ADD UNIQUE KEY `UNIQ_user_username` (`username`);

-- Optionnel : initialiser created_at/updated_at pour les lignes existantes
UPDATE `user`
   SET created_at = COALESCE(created_at, NOW()),
       updated_at = COALESCE(updated_at, NOW())
 WHERE created_at IS NULL OR updated_at IS NULL;

-- Vérification
SELECT id, email, username, roles, account_status, session_token,
       host_request_date, created_at, updated_at
  FROM `user`;
