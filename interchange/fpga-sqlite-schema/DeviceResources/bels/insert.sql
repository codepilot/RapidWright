insert into `bels` (`siteType`, `name`, `type`, `category`)
select ?1, ?2, ?3, `belCategories`.`rowid` from `belCategories` where `belCategories`.`category` = ?4
returning *;