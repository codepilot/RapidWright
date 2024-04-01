create table if not exists `enum_IOStandard` (
    `rowid` INTEGER PRIMARY KEY AUTOINCREMENT,
    `toString` TEXT NOT NULL UNIQUE
) strict;
