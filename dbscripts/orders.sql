CREATE TABLE IF NOT EXISTS baklava.orders (
  orderId INT(5) NOT NULL AUTO_INCREMENT,
  orderDate DATE NOT NULL,
  orderTime TIME NOT NULL,
  guestId INT(5) NOT NULL,
  waiterId INT(5) NOT NULL,
  restaurantId INT(5) NOT NULL,
  price DECIMAL(6, 2) NOT NULL,
  orderReady BOOLEAN DEFAULT 0,
  foodReady BOOLEAN DEFAULT 0,
  drinkReady BOOLEAN DEFAULT 0,
  CONSTRAINT `guest_ordUsrVDconstraint`
    FOREIGN KEY (guestId) REFERENCES baklava.users (userId),
  CONSTRAINT `waiter_ordUsrVDconstraint`
    FOREIGN KEY (waiterId) REFERENCES baklava.users (userId),
  CONSTRAINT `restaurant_ordUsrVDconstraint`
    FOREIGN KEY (restaurantId) REFERENCES baklava.restaurants (restaurantId),
  PRIMARY KEY (orderId)
);

CREATE TABLE IF NOT EXISTS baklava.orderVictualDrink (
  orderId INT(5) NOT NULL,
  victualDrinkId INT(5) NOT NULL,
  isReady BOOLEAN DEFAULT 0,
  accepted BOOLEAN DEFAULT 0,
  workerId INT(5) DEFAULT NULL,
  quantity INT(5) DEFAULT 1,
  CONSTRAINT `order_ordUsrVDconstraint`
    FOREIGN KEY (orderId) REFERENCES baklava.orders (orderId),
  CONSTRAINT `victualDrink_ordUsrVDconstraint`
    FOREIGN KEY (victualDrinkId) REFERENCES baklava.victualsanddrinks (victualsAndDrinksId),
  CONSTRAINT `cook_orderVDconstraint`
    FOREIGN KEY (workerId) REFERENCES baklava.workers (userId),
  PRIMARY KEY (orderId, victualDrinkId)
);

INSERT INTO baklava.orders (orderId, orderDate, orderTime, guestId, waiterId, restaurantId, price) VALUES
  (1, "2017-06-19", "19:24:22", 1, 6, 1, 1200.99);

INSERT INTO baklava.orderVictualDrink (orderId, victualDrinkId, workerId) VALUES
  (2, 4, 7);
INSERT INTO baklava.orderVictualDrink (orderId, victualDrinkId, workerId) VALUES
  (1, 2, 8);
INSERT INTO baklava.orderVictualDrink (orderId, victualDrinkId, workerId) VALUES
  (1, 3, 7);


SELECT DISTINCT o.guestId, u.email, uw.email, o.orderDate, o.orderTime, o.waiterId, vd.name, vd.description, vd.price
FROM baklava.orders AS o
  LEFT JOIN baklava.users AS u ON o.guestId = u.userId
  LEFT JOIN baklava.users AS uw ON o.waiterId = uw.userId
  LEFT JOIN baklava.orderVictualDrink AS ouvd ON o.orderId = ouvd.orderId
  LEFT JOIN baklava.victualsanddrinks AS vd ON ouvd.victualDrinkId = vd.victualsAndDrinksId
WHERE u.userId = 1;
