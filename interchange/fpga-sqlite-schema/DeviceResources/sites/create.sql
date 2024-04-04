create table if not exists `sites` (
    `rowid` INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` TEXT NOT NULL UNIQUE,
    `type` INTEGER NOT NULL,
    `tile` INTEGER NOT NULL,
    `x` INTEGER NOT NULL,
    `y` INTEGER NOT NULL,
    FOREIGN KEY(`type`) REFERENCES `enum_SiteTypeEnum`(`rowid`),
    FOREIGN KEY(`tile`) REFERENCES `tiles`(`rowid`)
) strict;
