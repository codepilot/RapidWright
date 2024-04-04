create table if not exists `nodes` (
    `rowid` INTEGER PRIMARY KEY AUTOINCREMENT,
    `tile` INTEGER NOT NULL,
    `wire` INTEGER NOT NULL,
    FOREIGN KEY(`tile`) REFERENCES `tiles`(`rowid`),
    FOREIGN KEY(`wire`) REFERENCES `tileTypeWires`(`rowid`),
    UNIQUE (`tile`, `wire`)
) strict;
