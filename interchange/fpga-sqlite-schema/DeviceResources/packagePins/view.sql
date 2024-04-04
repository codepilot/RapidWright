create view `view_packagePins`
as
select
    `packagePins`.`name` as `packagePinName`,
    `packagePins`.`bel`,
    `tiles`.`name` as `tileName`,
    `sites`.`name` as `siteName`,
    `enum_SiteTypeEnum`.`toString` as `siteType`,
    `enum_SiteTypeEnum`.`prefix`,
    `sites`.`x` as `siteX`,
    `sites`.`y` as `siteY`,
    `packages`.`name` as `packageName`
from
    `packagePins`
    join `sites` on `packagepins`.`site` = `sites`.`rowid`
    join `tiles` on `sites`.`tile` = `tiles`.`rowid`
    join `enum_SiteTypeEnum` on `sites`.`type` = `enum_SiteTypeEnum`.`rowid`
    join `packages` on `packagepins`.`package` = `packages`.`rowid`
;