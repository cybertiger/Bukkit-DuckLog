-- uuid <-> name mapping table
DROP TABLE IF EXISTS playernames;
CREATE TABLE playernames (
        uuid VARCHAR(36),
        name VARCHAR(16),
        last_seen BIGINT NOT NULL,
        PRIMARY KEY(uuid, name),
        INDEX (name, last_seen)
);

-- cumulative login time table
DROP TABLE IF EXISTS login_time;
CREATE TABLE login_time (
        uuid VARCHAR(36),
        server VARCHAR(100) NOT NULL,
        time BIGINT NOT NULL,
        PRIMARY KEY(uuid)
);

-- login_session, list of current player sessions
-- so we can account for current time logged in
DROP TABLE IF EXISTS login_sessions;
CREATE TABLE login_sessions (
        server VARCHAR(100) NOT NULL,
        uuid VARCHAR(36) NOT NULL,
        start BIGINT NOT NULL,
        PRIMARY KEY(uuid, server)
);

-- login_events used to figure out where a player was last.
DROP TABLE IF EXISTS login_events;
CREATE TABLE login_events (
        uuid VARCHAR(36) NOT NULL,
        server VARCHAR(100) NOT NULL,
        time BIGINT NOT NULL,
        ip VARCHAR(255) NOT NULL,
        type ENUM('LOGIN', 'LOGOUT') NOT NULL,
        INDEX (uuid, time),
        INDEX (ip, time)
);

-- version table
DROP TABLE IF EXISTS version;
CREATE TABLE version (
        id INT PRIMARY KEY NOT NULL
);
INSERT INTO version VALUES(1);