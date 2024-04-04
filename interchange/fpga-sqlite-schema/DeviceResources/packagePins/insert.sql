insert into `packagePins` (
    `name`, `package`, `site`, `bel`
) values (
    ?1,
    ?2,
    ?3,
    ?4
)
returning *;