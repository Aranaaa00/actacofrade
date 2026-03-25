-- ACTACOFRADE - V3: Fix event_clones foreign keys to cascade on delete

ALTER TABLE event_clones
    DROP CONSTRAINT event_clones_original_event_id_fkey,
    DROP CONSTRAINT event_clones_cloned_event_id_fkey;

ALTER TABLE event_clones
    ADD CONSTRAINT event_clones_original_event_id_fkey
        FOREIGN KEY (original_event_id) REFERENCES events(id) ON DELETE CASCADE,
    ADD CONSTRAINT event_clones_cloned_event_id_fkey
        FOREIGN KEY (cloned_event_id) REFERENCES events(id) ON DELETE CASCADE;
