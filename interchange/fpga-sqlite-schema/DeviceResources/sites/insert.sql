insert into `sites` (`name`, `type`, `tile`, `x`, `y`)
select
    ?1,
    `enum_SiteTypeEnum`.`rowid`,
    `tiles`.`rowid`,
    ?4,
    ?5
from
    `enum_SiteTypeEnum`,
    `tiles`
where
    `enum_SiteTypeEnum`.`toString` = ?2
and
    `tiles`.`name` = ?3
returning *;