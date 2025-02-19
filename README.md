# AAOS Speed Alert System

## Overview
I have developed an **Android Automotive OS (AAOS) project** designed to **alert car riders** when they exceed a predefined speed limit. 

## Features
### **1. Main Activity**
- The **MainActivity** provides fields to input:
  - Car Model
  - Driver Name
  - Speed Limit
- Upon submitting these details, the system starts a background service that **monitors vehicle speed** and alerts the driver when they exceed the threshold.
- Comments have been added in the code where notifications to the **fleet company** can be triggered.

### **2. Fleet Management with FleepApp**
- I have dedicated additional effort to integrating this project with a mobile application called **FleepApp** (available in this repository).
- **FleepApp allows fleet managers to add and manage multiple vehicles**.

### **3. Car Selection and Limit Management**
- A new activity, **CarListActivity**, has been introduced in the AAOS project.
- This activity enables users to:
  - **View the list of available cars**.
  - **Select a vehicle**, which dynamically updates the speed limit based on the selected car.
- In the future, this feature can be expanded to include **rider logins** for personalized settings.

## Future Enhancements
### **Integrating Firebase Cloud Functions**
- The next major improvement will involve using **Firebase Cloud Functions** to facilitate real-time communication between **AAOS and FleepApp**.
- This enhancement will enable **rental companies to receive instant speed alerts** whenever a vehicle exceeds its limit.
- The repository will continue to receive updates as new features are implemented.

---
Stay tuned for future updates! 🚀

