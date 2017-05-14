CREATE TABLE IF NOT EXISTS baklava.orders (
  orderId INT(5) NOT NULL AUTO_INCREMENT,
  orderDate DATE NOT NULL,
  orderTime TIME NOT NULL,
  guestId INT(5) NOT NULL,
  waiterId INT(5) NOT NULL,
  CONSTRAINT `guest_ordUsrVDconstraint`
    FOREIGN KEY (guestId) REFERENCES baklava.users (userId),
  CONSTRAINT `waiter_ordUsrVDconstraint`
    FOREIGN KEY (waiterId) REFERENCES baklava.users (userId),
  PRIMARY KEY (orderId)
);

CREATE TABLE IF NOT EXISTS baklava.orderuservictualdrink (
  orderId INT(5) NOT NULL,
  victualDrinkId INT(5) NOT NULL,
  CONSTRAINT `order_ordUsrVDconstraint`
    FOREIGN KEY (orderId) REFERENCES baklava.orders (orderId),
  CONSTRAINT `victualDrink_ordUsrVDconstraint`
    FOREIGN KEY (victualDrinkId) REFERENCES baklava.victualsanddrinks (victualsAndDrinksId)
);

INSERT INTO baklava.orders (orderDate, orderTime, guestId, waiterId) VALUES ("2017-05-14", "22:13:44", 1, 4);
INSERT INTO baklava.orders (orderDate, orderTime, guestId, waiterId) VALUES ("2017-05-13", "14:25:30", 1, 4);
INSERT INTO baklava.orders (orderDate, orderTime, guestId, waiterId) VALUES ("2017-05-10", "10:08:00", 2, 4);

INSERT INTO baklava.orderuservictualdrink VALUES (1, 3);
INSERT INTO baklava.orderuservictualdrink VALUES (1, 4);
INSERT INTO baklava.orderuservictualdrink VALUES (2, 3);

SELECT DISTINCT o.guestId, u.email, uw.email, o.orderDate, o.orderTime, o.waiterId, vd.name, vd.description, vd.price
FROM baklava.orders AS o
  LEFT JOIN users AS u ON o.guestId = u.userId
  LEFT JOIN users AS uw ON o.waiterId = uw.userId
  LEFT JOIN orderuservictualdrink AS ouvd ON o.orderId = ouvd.orderId
  LEFT JOIN victualsanddrinks AS vd ON ouvd.victualDrinkId = vd.victualsAndDrinksId
WHERE u.userId = 1;