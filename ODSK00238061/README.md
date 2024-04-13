# README

## Introduction

ODS is an Android application designed to assist visually impaired individuals in navigating their surroundings safely. Leveraging the power of Google's ML Kit and MobileNet, ODS employs custom machine learning models to detect obstacles in the user's environment in real-time.

## Features
1. Obstacle Detection - ODS utilises advanced pre-built TensorFlow Lite models to identify obstacles such as curbs, obstacles, and objects in the user's path.
2. Auditory Cues -  Upon detecting obstacles, ODS provides auditory alerts to notify users, ensuring timely awareness and avoidance.
3. Text-To-Speech - ODS also utilises Google ML Kit's Text Recognition to convert text from an image which reads back to the user
4. Barcode Scanner - ODS also offers the extraction of UPC values from a barcode and returns the name of the product, its details, ingredients or nutrional     values.
5. Voice Memo - ODS also allows users to record and play voice memos hands free.

## Installation

To install this project, follow these steps:

1. Open the following project in Android Studio.
2. Ensure your PC and the mobile device are using the same WiFi.
3. Click Device Manager > Pair Devices Using WiFi
4. In your mobile device, click Additional Settings > Developer Options
5. Select Wireless debugging > Pair Device with QR code
6. Scan the QR code provided by Android Studio
7. Once successfully paired, click 'Build' and 'Run' the application

## Usage

Here's how you can use this project:

Upon installation, the user can hold down on the screen and give the command they wish to navigate too. 
These commands include:
    - 'detect obstacles': opens the obstacle detection feature
    - 'text to speech': opens the text recognition feature.
        - 'read': reads the text back to the user
    - 'scan barcode': opens our barcode scanner feature
    - 'repeat': returns the product name (if available)
    - 'ingredients': returns the product ingredients (if available)
    - 'description': returns the product details (if availble)
    - 'nutrition': returns nutritional facts (if available)
    - 'product name': returns nutritional facts (if available) 


