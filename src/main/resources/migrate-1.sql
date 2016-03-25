CREATE TABLE login_time_new (
        uuid VARCHAR(36),
        server VARCHAR(100) NOT NULL,
        time BIGINT NOT NULL,
        PRIMARY KEY(uuid, server)
);

INSERT INTO login_time_new SELECT * FROM login_time;

DROP TABLE IF EXISTS login_time;

ALTER TABLE login_time_new RENAME TO login_time;

UPDATE version SET id = 2;
