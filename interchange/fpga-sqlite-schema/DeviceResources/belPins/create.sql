--   struct BELPin {
--     name   @0 : StringIdx $stringRef();
--     dir    @1 : Dir.Netlist.Direction;
--     bel    @2 : StringIdx $stringRef();
--   }
create table if not exists `belPins` (
    `rowid` INTEGER PRIMARY KEY AUTOINCREMENT,
    `siteType` INTEGER NOT NULL,
    `name` TEXT NOT NULL,
    `dir` INTEGER NOT NULL,
    `bel` TEXT NOT NULL,
    FOREIGN KEY(`siteType`) REFERENCES `siteTypeList`(`rowid`),
    FOREIGN KEY(`dir`) REFERENCES `enum_BELPin_Direction`(`rowid`)
) strict;