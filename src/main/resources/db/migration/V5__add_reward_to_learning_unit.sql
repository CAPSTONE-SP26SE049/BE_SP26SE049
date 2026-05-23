-- V5: Add reward_catalog_id FK to learning_unit (for QUIZ reward attachment)
ALTER TABLE learning_unit
    ADD COLUMN IF NOT EXISTS reward_catalog_id UUID;

ALTER TABLE learning_unit
    ADD CONSTRAINT fk_learning_unit_reward
        FOREIGN KEY (reward_catalog_id)
        REFERENCES reward_catalog(id)
        ON DELETE SET NULL;
