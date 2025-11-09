-- ======================================================
-- DATABASE
-- ======================================================
CREATE DATABASE IF NOT EXISTS minigo;
USE minigo;

-- ======================================================
-- TABLE: Locations
-- ======================================================
CREATE TABLE locations (
                           id INT AUTO_INCREMENT PRIMARY KEY,
                           name VARCHAR(100) NOT NULL UNIQUE
);

-- ======================================================
-- TABLE: Map Edges (Road Graph)
-- ======================================================
CREATE TABLE edges (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       from_id INT NOT NULL,
                       to_id INT NOT NULL,
                       distance_km DECIMAL(10,3) NOT NULL CHECK(distance_km > 0),
                       FOREIGN KEY (from_id) REFERENCES locations(id),
                       FOREIGN KEY (to_id) REFERENCES locations(id)
);

-- ======================================================
-- TABLE: Passengers
-- ======================================================
CREATE TABLE passengers (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            user_ssn VARCHAR(32) NOT NULL UNIQUE,
                            name VARCHAR(120) NOT NULL,
                            phone_number VARCHAR(32) NOT NULL,
                            email VARCHAR(160) NOT NULL,
                            wallet_balance DECIMAL(12,2) NOT NULL DEFAULT 0,
                            credit_balance DECIMAL(12,2) NOT NULL DEFAULT 0,

    -- Store location by STRING (place name)
                            current_location VARCHAR(160) NULL,

                            FOREIGN KEY (current_location) REFERENCES locations(name)
                                ON UPDATE CASCADE
                                ON DELETE SET NULL
);

-- ======================================================
-- TABLE: Drivers
-- ======================================================
CREATE TABLE drivers (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         user_ssn VARCHAR(32) NOT NULL UNIQUE,
                         name VARCHAR(120) NOT NULL,
                         phone_number VARCHAR(32) NOT NULL,
                         email VARCHAR(160) NOT NULL,
                         wallet_balance DECIMAL(12,2) NOT NULL DEFAULT 0,
                         credit_balance DECIMAL(12,2) NOT NULL DEFAULT 0,

    -- Store location by STRING (place name)
                         current_location VARCHAR(160) NULL,

                         license_plate VARCHAR(32) NOT NULL UNIQUE,
                         car_model VARCHAR(80) NOT NULL,
                         active BOOLEAN NOT NULL DEFAULT TRUE,

                         FOREIGN KEY (current_location) REFERENCES locations(name)
                             ON UPDATE CASCADE
                             ON DELETE SET NULL
);

-- ======================================================
-- TABLE: Ride Requests
-- ======================================================
CREATE TABLE ride_requests (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               passenger_id BIGINT NOT NULL,
                               driver_id BIGINT NULL,
                               origin_id INT NOT NULL,
                               destination_id INT NOT NULL,
                               status ENUM('Pending','Accepted','Cancelled','Completed') NOT NULL DEFAULT 'Pending',
                               distance_km DECIMAL(10,3) NOT NULL DEFAULT 0,
                               estimated_time INT NOT NULL DEFAULT 0,
                               estimated_price DECIMAL(12,2) NOT NULL DEFAULT 0,
                               acceptance_time DATETIME NULL,
                               driver_arrived BOOLEAN NOT NULL DEFAULT FALSE,
                               passenger_arrived BOOLEAN NOT NULL DEFAULT FALSE,
                               FOREIGN KEY(passenger_id) REFERENCES passengers(id),
                               FOREIGN KEY(driver_id) REFERENCES drivers(id),
                               FOREIGN KEY(origin_id) REFERENCES locations(id),
                               FOREIGN KEY(destination_id) REFERENCES locations(id)
);

-- ======================================================
-- TABLE: Ride History
-- ======================================================
CREATE TABLE ride_history (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              request_id BIGINT NOT NULL,
                              driver_id BIGINT NOT NULL,
                              passenger_id BIGINT NOT NULL,
                              passenger_rating TINYINT DEFAULT 0 CHECK(passenger_rating BETWEEN 0 AND 5),
                              driver_rating TINYINT DEFAULT 0 CHECK(driver_rating BETWEEN 0 AND 5),
                              ride_cost DECIMAL(12,2) NOT NULL DEFAULT 0,
                              payment_method ENUM('wallet','credit') NOT NULL,
                              tips DECIMAL(12,2) NOT NULL DEFAULT 0,
                              donation_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
                              donation_organization VARCHAR(160) NOT NULL DEFAULT '',
                              completed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              FOREIGN KEY(request_id) REFERENCES ride_requests(id),
                              FOREIGN KEY(driver_id) REFERENCES drivers(id),
                              FOREIGN KEY(passenger_id) REFERENCES passengers(id)
);

-- ======================================================
-- TABLE: Problem Types
-- ======================================================
CREATE TABLE problem_types (
                               id TINYINT PRIMARY KEY,
                               name VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO problem_types VALUES
                              (1,'DRIVER_BEHAVIOR'),
                              (2,'DRIVER_LATE'),
                              (3,'RECKLESS_DRIVING'),
                              (4,'VEHICLE_CLEANLINESS'),
                              (5,'TECHNICAL_ISSUE'),
                              (6,'FARE_DISPUTE'),
                              (7,'ACCOUNT_ISSUE');


-- ======================================================
-- TABLE: Problem Reports
-- ======================================================
CREATE TABLE problem_reports (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 request_id BIGINT NOT NULL,
                                 reporter_passenger_id BIGINT NOT NULL,
                                 driver_id BIGINT NULL,
                                 created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                 FOREIGN KEY(request_id) REFERENCES ride_requests(id),
                                 FOREIGN KEY(reporter_passenger_id) REFERENCES passengers(id),
                                 FOREIGN KEY(driver_id) REFERENCES drivers(id)
);

CREATE TABLE problem_report_types (
                                      report_id BIGINT NOT NULL,
                                      type_id TINYINT NOT NULL,
                                      details TEXT NOT NULL,
                                      PRIMARY KEY (report_id, type_id),
                                      FOREIGN KEY(report_id) REFERENCES problem_reports(id) ON DELETE CASCADE,
                                      FOREIGN KEY(type_id) REFERENCES problem_types(id)
);
