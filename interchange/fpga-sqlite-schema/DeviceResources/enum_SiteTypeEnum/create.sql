create table if not exists `enum_SiteTypeEnum` (
    `rowid` INTEGER PRIMARY KEY AUTOINCREMENT,
    `toString` TEXT NOT NULL UNIQUE,
    `prefix` TEXT NOT NULL
) strict;
