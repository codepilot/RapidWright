insert into `enum_SiteTypeEnum` (
    `toString`,
    `prefix`
) values (
    ?1,
    ?2
)
returning *;