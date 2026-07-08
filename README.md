# 🗓️ Task Scheduler

A **Java Swing-based desktop application** for scheduling and managing daily tasks with an intuitive graphical user interface. The application enables users to create, edit, delete, and monitor tasks while providing **automatic desktop notifications** at scheduled times. Task information is stored persistently in a **JSON file**, ensuring that scheduled tasks are restored even after restarting the application.

---

## 📖 Overview

Task Scheduler is a desktop productivity application developed using **Java Swing**. It demonstrates the use of Java GUI programming, concurrent task execution, file handling, and system tray integration.

The application allows users to schedule tasks by specifying a task name and execution time. Each task is executed automatically at the scheduled time, triggering a desktop notification. All task data is saved locally in JSON format, allowing users to continue where they left off after restarting the application.

This project was developed to demonstrate concepts such as event-driven programming, multithreading, persistent data storage, and desktop application development using Java.

---

# ✨ Features

- Create new scheduled tasks
- Edit existing tasks
- Delete scheduled tasks
- Automatic execution of tasks at the specified time
- Desktop notification using the System Tray
- Save tasks to a JSON file
- Automatically load saved tasks when the application starts
- Dark-themed graphical user interface
- Displays task status (Scheduled / Done)
- Uses UUIDs to uniquely identify tasks
- Responsive Swing-based interface
