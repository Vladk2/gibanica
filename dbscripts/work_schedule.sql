CREATE TABLE IF NOT EXISTS baklava.workingDay (
  dayId int(5) NOT NULL AUTO_INCREMENT,
  date DATE NOT NULL,
  startTime TIME NOT NULL,
  endTime TIME NOT NULL,
  PRIMARY KEY (dayID)
);

CREATE TABLE IF NOT EXISTS baklava.userWorkingDay (
  dayId int(5) NOT NULL,
  userId int(5) NOT NULL,
  CONSTRAINT `day_constraint`
    FOREIGN KEY (dayId) REFERENCES baklava.workingDay (dayId),
  CONSTRAINT `user_constraint`
    FOREIGN KEY (userId) REFERENCES baklava.users (userId)
);

INSERT INTO baklava.workingDay (date, startTime, endTime) values ("2017-05-11", "08:00:00", "16:00:00");
INSERT INTO baklava.workingDay (date, startTime, endTime) values ("2017-05-11", "16:00:00", "00:00:00");

INSERT INTO baklava.userWorkingDay values (1, 4);