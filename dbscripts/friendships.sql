create table if not exists baklava.friendships (
    id                      bigint not null auto_increment primary key,
    friendid1               int not null,
    friendid2               int not null,
    time_req_sent           datetime,
    time_req_accepted       datetime,
    accepted                boolean default 0,
    deleted                 boolean default 0,
    constraint `friendid1_constraint`
        foreign key (friendid1) references baklava.users (userId),
    constraint `friendid2_constraint`
        foreign key (friendid2) references baklava.users (userId),
    constraint `friendids_not_same_constraint`
        check (friendid1 != friendid2)
);

