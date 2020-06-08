CREATE TABLE smManuellBehandling (
    sykmelding_id CHAR(64) NOT NULL,
    created TIMESTAMP NOT NULL,
    data jsonb NOT NULL
);

CREATE TABLE smAutomatiskBehandling (
    sykmelding_id CHAR(64) NOT NULL,
    created TIMESTAMP NOT NULL,
    data jsonb NOT NULL
);

CREATE TABLE smHist (
    sykmelding_id CHAR(64) NOT NULL,
    created TIMESTAMP NOT NULL,
    data jsonb NOT NULL
);

CREATE TABLE smBehandlingsutfall (
    sykmelding_id CHAR(64) NOT NULL,
    created TIMESTAMP NOT NULL,
    data jsonb NOT NULL
);

CREATE TABLE smBehandlingsutfallHist (
    sykmelding_id CHAR(64) NOT NULL,
    created TIMESTAMP NOT NULL,
    data jsonb NOT NULL
);

CREATE TABLE smSykmeldingstatus (
    sykmelding_id CHAR(64) NOT NULL,
    created TIMESTAMP NOT NULL,
    data jsonb NOT NULL
);

CREATE TABLE smSykmeldingstatusHist (
    sykmelding_id CHAR(64) NOT NULL,
    created TIMESTAMP NOT NULL,
    data jsonb NOT NULL
);
