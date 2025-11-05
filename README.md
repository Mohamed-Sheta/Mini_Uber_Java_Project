# 🚗 MiniGo

## 📘 Overview
**mini_uber_system** is a simplified ride-sharing management system inspired by Uber.  
It models the interactions between **passengers**, **drivers**, and **ride managers**, covering ride requests, payments, locations, and issue reporting.

The system is designed in **Java** using **Object-Oriented Programming (OOP)** principles, and data is persisted using **MySQL** via **JDBC** connections.

---

## 🧩 Class Descriptions

### 1. Person (Base Class)
**Attributes:**  
`userSSN`, `name`, `phoneNumber`, `email`, `walletBalance`, `creditBalance`, `rideHistory`, `ratingAccount`  

**Methods:**  
`showProfile()`, `getAverageRate()`, `addRideHistory()`  

**Description:**  
Represents a general user in the system (either a Passenger or a Driver).

---

### 2. Passenger (extends Person)
**Attributes:**  
`currentLocation`, `destination`  

**Methods:**  
`requestRide()`, `cancelRide()`, `rateDriver()`, `reportProblem()`, `set_WalletBalance()`, `set_creditBalance()`  

**Description:**  
A customer who requests rides, pays fares, and can report issues or rate drivers.

---

### 3. Driver (extends Person)
**Attributes:**  
`licensePlate`, `carModel`, `active`, `currentLocation`  

**Methods:**  
`viewRideRequests()`, `acceptRequest()`, `ratePassenger()`  

**Description:**  
A driver who can accept ride requests, manage availability, and receive ratings.

---

### 4. RideManager
**Attributes:**  
`availableDrivers`, `request`  

**Methods:**  
`createRide()`, `assignNearestDriver()`, `addCompletedRide()`  

**Description:**  
Handles ride creation, driver assignment, and management of completed rides.

---

### 5. Request
**Attributes:**  
`requestId`, `passenger`, `origin`, `destination`, `status`, `distance`, `estimatedTime`, `payment`  

**Methods:**  
`getRequest()`, `estimated_price`,`estimated_distance`

**Description:**  
Represents a ride request, storing trip details and payment information.

---

### 6. RideHistory
**Attributes:**  
`historyId`, `request`, `driver`, `passenger`  

**Methods:**  
`getHistoryDetails()`, `getRideCounts()`  

**Description:**  
Keeps a record of completed rides for each passenger and driver.

---

### 7. Payment
**Attributes:**  
`paymentId`, `amount`, `paymentMethod`, `option`  

**Methods:**  
`processPayment()`, `getPaymentDetails()`  

**Description:**  
Handles fare transactions and connects to **Options** for extra features like tips or donations.

---

### 8. Options
**Attributes:**  
`tips`, `donationAmount`, `donationOrganization`, `isTipsEnabled`, `isDonationEnabled`  

**Methods:**  
`enableTips()`, `enableDonation()`, `setTipsAmount()`, `giveDonation()`  

**Description:**  
Provides passengers with optional payment features such as tipping or donating to an organization.

---

### 9. Problems
**Attributes:**  
`reportId`, `manage`, `types`, `details`  

**Description:**  
Manages problem reports related to driver behavior, disputes, or technical issues.  

**Enum:**  
`ProblemType` includes issues like `DRIVER_BEHAVIOR`, `VEHICLE_CLEANLINESS`, and `FARE_DISPUTE`.

---

### 10. Location
**Attributes:**  
 `name`  

**Methods:**  
 `getName()`  

**Description:**  
Represents a physical point used for ride origins and destinations.

---

### 11. Edge
**Attributes:**  
`from`, `to`, `distance`

**Methods:**  
`getToLocation()`, `getDistance()` 

**Description:**  
Represents a path between two locations.

---

### 12. MapGraph
**Attributes:**  
`adjacencyList`  

**Methods:**  
`addLocation()`, `addEdge()`, `dijkstraShortestPath()`  

**Description:**  
Models the city map and calculates the shortest route between two locations using Dijkstra’s algorithm.

---

## 🔄 System Workflow

1. A **Passenger** uses `requestRide()` to create a new ride request.  
2. The **RideManager** assigns the **nearest available Driver**.  
3. The **Driver** can accept the ride request. 
4. Once accepted, the **Request** object is created linking passenger, driver, and route details.  
5. When the trip is completed:
   - **Payment** is processed using `update_process_Payment()`.  
   - Optional **tips** or **donations** are added via **Options**.  
   - The trip is stored in **RideHistory**.  
6. The **Passenger** can rate the **Driver** or report issues through **Problems**.  
7. All data is stored and retrieved from the **MySQL** database using **JDBC**.

---

## 🗃️ Database Integration (MySQL + JDBC)

- The system connects to a **MySQL** database using **Java JDBC**.  
- Each class corresponds to a table in the database:  
  `Passenger`, `Driver`, `Request`, `Payment`, `RideHistory`, etc.

**CRUD Operations:**
- `INSERT` — Add new rides, drivers, or payments.  
- `SELECT` — Retrieve ride history or user details.  
- `UPDATE` — Modify wallet balances or ride status.  
- `DELETE` — Remove canceled or invalid records.  

**JDBC handles:**
- Connection setup using `DriverManager.getConnection()`.  
- Prepared statements for SQL queries.  
- ResultSet management for query results.  

---

## 🛠️ Technologies Used
- **Java (OOP + JDBC)**  
- **MySQL (Database)**  
- **UML Modeling (Visual Paradigm)**  
- **Dijkstra Algorithm (for route calculation)**
## 🎨 UI/UX
**Glide Route Pro App – MiniGO UI/UX**  
Explore the user interface and experience design here: [MiniGO UI/UX]([https://glide-route-pro.lovable.app/?utm_source=chatgpt.com](https://uml-dark-mode-magic.lovable.app/))
