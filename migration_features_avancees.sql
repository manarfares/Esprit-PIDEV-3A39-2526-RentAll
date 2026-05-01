-- =====================================================================
-- Migration non-breaking : colonnes nécessaires aux features avancées
-- (confirmation email, reset SMS, OAuth)
-- ⚠️  À exécuter UNE SEULE FOIS dans phpMyAdmin sur la base pidev_amine
-- Toutes les colonnes sont nullables → Symfony n'est pas cassé
-- =====================================================================

ALTER TABLE `user`
    ADD COLUMN `confirmation_token`         VARCHAR(255) NULL DEFAULT NULL,
    ADD COLUMN `confirmation_token_expires` DATETIME     NULL DEFAULT NULL,
    ADD COLUMN `reset_sms_code`             VARCHAR(10)  NULL DEFAULT NULL,
    ADD COLUMN `reset_sms_expires`          DATETIME     NULL DEFAULT NULL,
    ADD COLUMN `oauth_provider`             VARCHAR(30)  NULL DEFAULT NULL,
    ADD COLUMN `oauth_id`                   VARCHAR(100) NULL DEFAULT NULL;

-- Index pour lookup rapide lors de la confirmation / OAuth
CREATE INDEX `idx_user_confirmation_token` ON `user`(`confirmation_token`);
CREATE INDEX `idx_user_oauth` ON `user`(`oauth_provider`, `oauth_id`);
