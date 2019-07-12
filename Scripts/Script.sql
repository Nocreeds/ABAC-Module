
CREATE TABLE IF NOT EXISTS smartlock
(
    ID VARCHAR(4) NOT NULL,
    state TINYINT(1),
    PRIMARY KEY(ID)
),

CREATE TABLE IF NOT EXISTS firesensor
(
    ID VARCHAR(4) NOT NULL,
    state TINYINT(1),
    PRIMARY KEY(ID)
),

CREATE TABLE IF NOT EXISTS bloodpressur
(
    ID VARCHAR(4) NOT NULL,
    pressur FLOAT(6),
    PRIMARY KEY(ID)
),

CREATE TABLE IF NOT EXISTS camera
(
    ID VARCHAR(4) NOT NULL,
    link VARCHAR(50),
    PRIMARY KEY(ID)
),

CREATE TABLE IF NOT EXISTS smartlock_log
(
    ID_user VARCHAR(4) NOT NULL,
    ID_smartlock VARCHAR(4) NOT NULL,
    PRIMARY KEY(ID_user, ID_smartlock),
    FOREIGN KEY (ID_smartlock)
    REFERENCES smartlock(ID),
    FOREIGN KEY (ID_user)
    REFERENCES utilisateurs(ID)
),

CREATE TABLE IF NOT EXISTS firesensor_log
(
    ID_user VARCHAR(4) NOT NULL,
    ID_firesensor VARCHAR(4) NOT NULL,
    PRIMARY KEY(ID_user, ID_firesensor),
    FOREIGN KEY (ID_user)
    REFERENCES utilisateurs(ID),
    FOREIGN KEY (ID_firesensor)
    REFERENCES firesensor(ID)

),

CREATE TABLE IF NOT EXISTS bloodpressur_log
(
    ID_user VARCHAR(4) NOT NULL,
    ID_bloodpressur VARCHAR(4) NOT NULL,
    PRIMARY KEY(ID_user, ID_bloodpressur),
    FOREIGN KEY (ID_user)
    REFERENCES utilisateurs(ID),
    FOREIGN KEY (ID_bloodpressur)
    REFERENCES bloodpressur(ID)

),

CREATE TABLE IF NOT EXISTS camera_log
(
    ID_user VARCHAR(4) NOT NULL,
    ID_camera VARCHAR(4) NOT NULL,
    PRIMARY KEY(ID_user, ID_camera),
    FOREIGN KEY (ID_user)
    REFERENCES utilisateurs(ID),
    FOREIGN KEY (ID_camera)
    REFERENCES camera(ID)

);