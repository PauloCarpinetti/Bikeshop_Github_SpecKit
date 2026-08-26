-- Fase 5C: bloqueio de cadastro de cliente pelo backoffice (FR-009).

ALTER TABLE cliente ADD COLUMN bloqueado BOOLEAN NOT NULL DEFAULT FALSE;
