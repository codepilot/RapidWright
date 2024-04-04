create view if not exists `view_bels` (
    `rowid`,
    `siteType`,
    `name`,
    `type`,
    `category`)
as
select
    `bels`.`rowid`,
    `enum_SiteTypeEnum`.`toString`,
    `bels`.`name`,
    `bels`.`type`,
    `belCategories`.`category`
from `bels`
join `belCategories` on `bels`.`category` = `belCategories`.`rowid`
join `enum_SiteTypeEnum` on `bels`.`siteType` = `enum_SiteTypeEnum`.`rowid`
;
