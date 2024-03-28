-- siteTypeList    @2 : List(SiteType);
create table if not exists `siteTypeList` (
    `rowid` INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` INTEGER, -- name         @0 : StringIdx $stringRef();
-- belPins      @1 : List(BELPin); # All BEL Pins in site type
-- pins         @2 : List(SitePin);
-- lastInput    @3 : UInt32; # Index of the last input pin
-- bels         @4 : List(BEL);
-- sitePIPs     @5 : List(SitePIP);
-- siteWires    @6 : List(SiteWire);
-- altSiteTypes @7 : List(SiteTypeIdx);
    FOREIGN KEY(`name`) REFERENCES `strList`(`rowid`)
) strict;
