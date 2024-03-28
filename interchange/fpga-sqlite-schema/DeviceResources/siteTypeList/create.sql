-- siteTypeList    @2 : List(SiteType);
create table if not exists `siteTypeList` (
    `rowid` INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` TEXT NOT NULL UNIQUE
) strict;
