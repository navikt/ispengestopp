CREATE TABLE status_endring (
  id                       SERIAL             PRIMARY KEY,
  uuid                     VARCHAR(50)        NOT NULL UNIQUE,
  sykmeldt_fnr             VARCHAR(11)        NOT NULL,
  veileder_ident           VARCHAR(7)         NOT NULL,
  status                   VARCHAR(99)        NOT NULL,
  virksomhet_nr            VARCHAR(9)         NOT NULL,
  timestamptz              TIMESTAMP          NOT NULL
);
