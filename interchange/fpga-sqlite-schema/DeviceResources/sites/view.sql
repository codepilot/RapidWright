create view if not exists `view_sites` (
    `rowid`,
    `name`,
    `type`,
    `tile`
)
as
select
    `sites`.`rowid`,
    `sites`.`name`,
    `enum_SiteTypeEnum`.`toString`,
    `tiles`.`name`
from
    `sites`
    join `enum_SiteTypeEnum` on `sites`.`type` = `enum_SiteTypeEnum`.`rowid`
    join `tiles` on `sites`.`tile` = `tiles`.`rowid`
;