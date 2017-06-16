CREATE TABLE IF NOT EXISTS baklava.visits(
  visitId INT(5) NOT NULL AUTO_INCREMENT,
  userId INT(5) NOT NULL,
  restaurantId INT(5) NOT NULL,
  CONSTRAINT `visit_user`
  FOREIGN KEY (userId) REFERENCES baklava.users (userId),
  CONSTRAINT `visit_restaurant`
  FOREIGN KEY (restaurantId) REFERENCES baklava.restaurants (restaurantId),
  PRIMARY KEY (visitId)
);

-- treba dodati datum, vreme posete

CREATE TABLE IF NOT EXISTS baklava.restaurantsRating(
  userId INT(5) NOT NULL,
  restaurantId INT(5) NOT NULL,
  visitId INT(5) NOT NULL,
  rating INT(1) DEFAULT 0,
  serviceRating INT(1) DEFAULT 0,
  mealRating INT(1) DEFAULT 0,
  CONSTRAINT `restaurantRating_user`
  FOREIGN KEY (userId) REFERENCES baklava.users (userId),
  CONSTRAINT `restaurantRating_restaurant`
  FOREIGN KEY (restaurantId) REFERENCES baklava.restaurants (restaurantId),
  CONSTRAINT `restaunrantRating_visit`
  FOREIGN KEY (visitId) REFERENCES baklava.visits (visitId),
  PRIMARY KEY (userId, restaurantId, visitId)
);