create table if not exists `enum_BELPin_Direction` (
    `rowid` INTEGER PRIMARY KEY AUTOINCREMENT,
    `toString` TEXT NOT NULL UNIQUE
) strict;
