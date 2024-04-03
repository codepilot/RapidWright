create table if not exists `enum_IntentCode` (
    `rowid` INTEGER PRIMARY KEY AUTOINCREMENT,
    `toString` TEXT NOT NULL UNIQUE,
    `category` INTEGER,
    FOREIGN KEY(`category`) REFERENCES `enum_WireCategory`(`rowid`)
) strict;
