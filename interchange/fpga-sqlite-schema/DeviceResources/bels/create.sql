-- bels         @4 : List(BEL);
create table if not exists `bels` (
    `rowid` INTEGER PRIMARY KEY AUTOINCREMENT,
    `siteType` INTEGER NOT NULL,
    `name` TEXT NOT NULL,
    `type` TEXT NOT NULL,
    `category` INTEGER NOT NULL,
    FOREIGN KEY(`siteType`) REFERENCES `enum_SiteTypeEnum`(`rowid`),
    FOREIGN KEY(`category`) REFERENCES `belCategories`(`rowid`)
) strict;
