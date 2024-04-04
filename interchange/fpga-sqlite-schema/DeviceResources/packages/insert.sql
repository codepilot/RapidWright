insert into `packages` (
    `name`
) values (
    ?1
)
returning *;