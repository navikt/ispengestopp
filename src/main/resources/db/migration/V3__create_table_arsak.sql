CREATE TABLE ARSAK (
  id                       SERIAL             PRIMARY KEY,
  uuid                     VARCHAR(50)        NOT NULL UNIQUE,
  status_endring_id        INTEGER REFERENCES STATUS_ENDRING (id) ON DELETE CASCADE,
  arsaktype                VARCHAR(50)        NOT NULL,
  opprettet                TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP
);
