create table if not exists baklava.reservations (
    reservationId int(5) primary key auto_increment,
    userId int(5) not null,
    restaurantId int(5) not null,
    startTimestamp timestamp,
    endTimestamp timestamp,
    constraint `userId_constraint_reservations`
        foreign key(userId) references baklava.users(userId),
    constraint `restaurantId_constraint_reservations`
        foreign key(restaurantId) references baklava.restaurants(restaurantId)
);

create table if not exists baklava.reservationSeats (
    reservationId int(5) not null,
    seatId int(12) not null,
    constraint `reservationId_constraint_tables`
        foreign key(reservationId) references baklava.reservations(reservationId),
    constraint `seatId_constraint_tables`
        foreign key(seatId) references baklava.seatConfig(seatId),
    primary key(reservationId, seatId)
);

-- pozvani korisnici
create table if not exists baklava.reservationGuests (
    reservationId int(5) not null,
    userId int(5) not null,
    confirmed boolean not null default 0,
    constraint `userId_constraint_guests`
        foreign key(userId) references baklava.users(userId),
    constraint `reservationId_constraint_guests`
        foreign key(reservationId) references baklava.reservations(reservationId),
    primary key(reservationId, userId)
);

-- treba i tabela za porudzbine, to kasnije
-- userId, reservationId, orderId iz orders, boolean prepareBeforeArrival default 0

create table if not exists baklava.reservationOrders (
    reservationId int(5) not null,
    userId int(5) not null,
    orderId int(5) not null,
    prepareBeforeArrival boolean not null default 0,
    constraint `resIdContstraint`
        foreign key(reservationId) references baklava.reservations(reservationId),
    constraint `userIdConstraint`
        foreign key(userId) references baklava.users(userId),
    constraint `orderIdConstraint`
        foreign key(orderId) references baklava.orders(orderId)
);
