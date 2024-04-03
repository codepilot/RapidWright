insert into `enum_IntentCode` (`toString`, `category`)
select ?1, `enum_WireCategory`.`rowid` from `enum_WireCategory` where `enum_WireCategory`.`toString` = ?2
returning *;