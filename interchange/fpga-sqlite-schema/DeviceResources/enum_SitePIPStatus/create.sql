create table if not exists `enum_SitePIPStatus` (
    `rowid` INTEGER PRIMARY KEY AUTOINCREMENT,
    `toString` TEXT NOT NULL UNIQUE
) strict;
