create table if not exists `belCategories` (
    `rowid` INTEGER PRIMARY KEY AUTOINCREMENT,
    `category` TEXT NOT NULL UNIQUE
) strict;
