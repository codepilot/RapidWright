-- strList         @1 : List(Text) $hashSet();
create table if not exists `strList` (
    `rowid` INTEGER PRIMARY KEY AUTOINCREMENT,
    `str` TEXT NOT NULL UNIQUE
) strict;