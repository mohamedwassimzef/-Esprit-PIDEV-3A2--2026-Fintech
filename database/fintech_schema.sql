-- =============================================================
--  Fintech Application - Full Database Schema
--  Generated to match all DAO / Entity classes
--  Database: fintech
-- =============================================================

CREATE DATABASE IF NOT EXISTS `fintech`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE `fintech`;

-- =============================================================
-- 1. role
-- =============================================================
CREATE TABLE IF NOT EXISTS `role` (
  `id`          INT         NOT NULL AUTO_INCREMENT,
  `role_name`   VARCHAR(50) NOT NULL,
  `permissions` TEXT        DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================================
-- 2. user
-- =============================================================
CREATE TABLE IF NOT EXISTS `user` (
  `id`            INT          NOT NULL AUTO_INCREMENT,
  `name`          VARCHAR(100) NOT NULL,
  `email`         VARCHAR(150) NOT NULL UNIQUE,
  `password_hash` VARCHAR(255) NOT NULL,
  `role_id`       INT          NOT NULL DEFAULT 2,
  `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_verified`   TINYINT(1)   NOT NULL DEFAULT 0,
  `phone`         VARCHAR(30)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`role_id`) REFERENCES `role`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================================
-- 3. insured_asset
-- ============================================================

-- =============================================================
--  Seed Data - Roles
-- =============================================================
INSERT IGNORE INTO `role` (`id`, `role_name`, `permissions`) VALUES
  (1, 'admin', '{"all": true}'),
  (2, 'user',  '{"assets": true, "contracts": true, "loans": true}');

-- =============================================================
--  Seed Data - Insurance Packages
-- =============================================================
INSERT IGNORE INTO `insurance_package`
  (`name`, `asset_type`, `description`, `coverage_details`, `base_price`, `risk_multiplier`, `duration_months`, `is_active`)
VALUES
  ('Basic Car Cover',    'car',  'Covers third-party liability for vehicles.',      'Third-party liability up to 50000 TND.',          250.00, 1.00, 12, 1),
  ('Premium Car Cover',  'car',  'Full comprehensive coverage for vehicles.',       'Comprehensive coverage up to 200000 TND.',         750.00, 1.50, 12, 1),
  ('Basic Home Cover',   'home', 'Covers fire and natural disasters for homes.',    'Fire, flood and earthquake up to 100000 TND.',    300.00, 1.00, 12, 1),
  ('Premium Home Cover', 'home', 'Full protection for residential properties.',     'All-risk coverage up to 500000 TND.',              900.00, 1.80, 12, 1),
  ('Annual Car Plan',    'car',  'Annual plan with roadside assistance.',            'Roadside assistance and third-party up to 80000 TND.', 400.00, 1.20, 12, 1),
  ('Annual Home Plan',   'home', 'Annual home insurance with theft coverage.',      'Theft, fire and flood up to 250000 TND.',         600.00, 1.30, 12, 1);

-- =============================================================
-- Migration: add boldsign_document_id to contract_request
-- Run this if the table already exists in your database.
-- =============================================================
ALTER TABLE `contract_request`
  ADD COLUMN IF NOT EXISTS `boldsign_document_id` VARCHAR(255) DEFAULT NULL;

-- =============================================================
-- Note: the contract_request.status column accepts these values:
--   PENDING | APPROVED | REJECTED | SIGNED
-- No schema change is needed since it is stored as VARCHAR(20).
-- =============================================================

