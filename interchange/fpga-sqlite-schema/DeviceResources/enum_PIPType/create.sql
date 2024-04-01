create table if not exists `enum_PIPType` (
    `rowid` INTEGER PRIMARY KEY AUTOINCREMENT,
    `toString` TEXT NOT NULL UNIQUE
) strict;
