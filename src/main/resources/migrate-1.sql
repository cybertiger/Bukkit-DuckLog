CREATE TABLE login_time_new (
        uuid VARCHAR(36),
        server VARCHAR(100) NOT NULL,
        time BIGINT NOT NULL,
        PRIMARY KEY(uuid, server)
);

INSERT INTO login_time_new SELECT * FROM login_time;

DROP TABLE IF EXISTS login_time;

ALTER TABLE login_time_new RENAME TO login_time;

-- last seen table
DROP TABLE IF EXISTS last_seen;
CREATE TABLE last_seen (
        uuid VARCHAR(36) NOT NULL,
        server VARCHAR(100) NOT NULL,
        time BIGINT NOT NULL,
        ip VARCHAR(255),
        world VARCHAR(80),
        x int,
        y int,
        z int,
        PRIMARY KEY(uuid, server)
);

INSERT INTO last_seen (uuid, server, time, ip) 
SELECT t1.uuid, t1.server, t1.time, t1.ip FROM login_events t1, (select uuid, server, max(time) as time from login_events group by uuid, server) as t2 where t1.uuid = t2.uuid and t1.time = t2.time;

UPDATE version SET id = 2;