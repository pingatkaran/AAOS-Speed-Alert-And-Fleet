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

### **2. Fleet Management with FleetApp**  
- I have dedicated additional effort to integrating this project with a mobile application called **FleetApp** (available in this repository).  
- **FleetApp allows fleet managers to add and manage multiple vehicles**.  

### **3. Car Selection and Limit Management**  
- A new activity, **CarListActivity**, has been introduced in the AAOS project.  
- This activity enables users to:  
  - **View the list of available cars**.  
  - **Select a vehicle**, which dynamically updates the speed limit based on the selected car.  
- In the future, this feature can be expanded to include **rider logins** for personalized settings.  

### **4. Fleet Notifications via Push Notifications** ✅  
- The **FleetApp** will now receive **push notifications** whenever a rider **exceeds the speed limit**.  
- The system uses **Firebase Cloud Messaging (FCM)** and **AWS SNS** as communication channels.  
- **Notifications include rider details**, enabling **fleet managers to take immediate action**.

## File

[SpeedService.kt](https://github.com/pingatkaran/AAOS-Speed-Alert-And-Fleet/blob/main/automotive/src/main/java/com/app/drivealert/SpeedService.kt)

## Video  

[![Watch the video](https://img.youtube.com/vi/6lrdkMmy3V0/maxresdefault.jpg)](https://www.youtube.com/watch?v=6lrdkMmy3V0)  

---  
Stay tuned for future updates! 🚀

