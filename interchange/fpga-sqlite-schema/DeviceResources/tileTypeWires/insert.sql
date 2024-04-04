insert into `tileTypeWires` (`name`, `type`, `tileType`)
select
    ?1,
    `enum_IntentCode`.`rowid`,
    `enum_TileTypeEnum`.`rowid`
from
    `enum_IntentCode`,
    `enum_TileTypeEnum`
where
    `enum_IntentCode`.`toString` = ?2
and
    `enum_TileTypeEnum`.`toString` = ?3
returning *;