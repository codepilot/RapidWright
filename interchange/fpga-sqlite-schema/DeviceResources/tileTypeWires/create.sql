create table if not exists `tileTypeWires` (
    `rowid` INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` TEXT NOT NULL,
    `type` INTEGER NOT NULL,
    `tileType` INTEGER NOT NULL,
    FOREIGN KEY(`type`) REFERENCES `enum_IntentCode`(`rowid`),
    FOREIGN KEY(`tileType`) REFERENCES `enum_TileTypeEnum`(`rowid`)
) strict;