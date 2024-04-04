create table if not exists `packagePins` (
    `rowid` INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` TEXT NOT NULL,
    `package` INTEGER NOT NULL REFERENCES `package`(`rowid`),
    `site` INTEGER REFERENCES `sites`(`rowid`),
    `bel` TEXT,
    UNIQUE(`package`, `name`)
) strict;
