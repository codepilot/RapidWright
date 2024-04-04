create view if not exists `view_tileTypeWires` (
    `rowid`,
    `name`,
    `type`,
    `tileType`)
as
select
    `tileTypeWires`.`rowid`,
    `tileTypeWires`.`name`,
    `enum_IntentCode`.`toString`,
    `enum_TileTypeEnum`.`toString`

from `tileTypeWires`
join `enum_IntentCode` on `tileTypeWires`.`type` = `enum_IntentCode`.`rowid`
join `enum_TileTypeEnum` on `tileTypeWires`.`tileType` = `enum_TileTypeEnum`.`rowid`
;