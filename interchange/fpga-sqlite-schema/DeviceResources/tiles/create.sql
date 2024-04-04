create table if not exists `tiles` (
    `rowid` INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` TEXT NOT NULL UNIQUE,
    `type` INTEGER NOT NULL,
    `row` INTEGER NOT NULL,
    `col` INTEGER NOT NULL,
    `x` INTEGER NOT NULL,
    `y` INTEGER NOT NULL,
    FOREIGN KEY(`type`) REFERENCES `enum_TileTypeEnum`(`rowid`),
    UNIQUE (`row`, `col`)
) strict;
