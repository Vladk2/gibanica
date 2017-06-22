create table if not exists baklava.reservations (
    reservationId int(5) primary key auto_increment,
    userId int(5) not null,
    restaurantId int(5) not null,
    date date not null,
    startTime time not null,
    endTime time not null,
    constraint `userId_constraint`
        foreign key(userId) references baklava.users(userId),
    constraint `restaurantId_constraint`
        foreign key(restaurantId) references baklava.restaurants(restaurantId)
);

create table if not exists baklava.reservationTables (
    reservationId int(5) not null,
    seatId int(12) not null,
    constraint `reservationId_constraint`
        foreign key(reservationId) references baklava.reservations(reservationId),
    constraint `seatId_constraint`
        foreign key(seatId) references baklava.seatconfig(seatId),
    primary key(reservationId, seatId)
);

-- pozvani korisnici
create table if not exists baklava.reservationGuests (
    reservationId int(5) not null,
    userId int(5) not null,
    confirmed boolean not null default 0,
    constraint `userId_constraint`
        foreign key(userId) references baklava.users(userId),
    constraint `reservationId_constraint`
        foreign key(reservationId) references baklava.reservations(reservationId),
    primary key(reservationId, userId)
);

-- treba i tabela za porudzbine, to kasnije
-- userId, reservationId, orderId iz orders, boolean prepareBeforeArrival default 0
