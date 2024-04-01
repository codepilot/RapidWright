insert into `belPins` (`siteType`, `name`, `dir`, `bel`)
select ?1, ?2, `enum_BELPin_Direction`.`rowid`, ?4 from `enum_BELPin_Direction` where `enum_BELPin_Direction`.`toString` = ?3
returning *;