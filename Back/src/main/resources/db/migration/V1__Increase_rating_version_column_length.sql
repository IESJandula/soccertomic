-- Migration: Increase rating_version column length from 20 to 50 characters
-- Reason: Support longer rating version strings like "trueskill-adapted-v2-selfinit"
ALTER TABLE Usuario MODIFY COLUMN rating_version VARCHAR(50) NOT NULL;
