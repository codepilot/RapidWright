create table if not exists `nodeWires` (
    `rowid` INTEGER PRIMARY KEY AUTOINCREMENT,
    `node` INTEGER NOT NULL,
    `tile` INTEGER NOT NULL,
    `wire` INTEGER NOT NULL,
    FOREIGN KEY(`node`) REFERENCES `nodes`(`rowid`),
    FOREIGN KEY(`tile`) REFERENCES `tiles`(`rowid`),
    FOREIGN KEY(`wire`) REFERENCES `tileTypeWires`(`rowid`),
    UNIQUE (`tile`, `wire`)
) strict;
