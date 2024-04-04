create view if not exists `view_tiles` (
    `rowid`,
    `name`,
    `type`,
    `row`,
    `col`)
as
select
    `tiles`.`rowid`,
    `tiles`.`name`,
    `enum_TileTypeEnum`.`toString`,
    `tiles`.`row`,
    `tiles`.`col`

from `tiles`
join `enum_TileTypeEnum` on `tiles`.`type` = `enum_TileTypeEnum`.`rowid`
;
