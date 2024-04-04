insert into `nodeWires` (`node`,`tile`, `wire`)
values(?1, ?2, ?3)
returning *;