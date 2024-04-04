insert into `tiles` (`name`, `type`, `row`, `col`, `x`, `y`)
select
    ?1,
    `enum_TileTypeEnum`.`rowid`,
    ?3,
    ?4,
    ?5,
    ?6
from
    `enum_TileTypeEnum`
where
    `enum_TileTypeEnum`.`toString` = ?2
returning *;