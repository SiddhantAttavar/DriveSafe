# DriveSafe
DriveSafe: A smartphone app to monitor reckless driving behaviours

## What is the problem?: <a name = "problem"></a>
Road accidents killed 13.5 lakh people globally in 2018. They are the single largest cause of death for those in age group of 15-45. Reckless driving is responsible for 90% of road deaths.

## My solution: <a name = "solution"></a>
I have developed a smartphone application, DriveSafe, which detects reckless driving behaviours in real-time.

## Steps for development: <a name = "steps-for-development"></a>
I have decided to use a smartphone as it has various advantages including cost-effectiveness, and wide accessibility.

I identified the major reckless driving behaviours. These are:
 - Overspeeding
 - Sudden braking
 - Sudden acceleration
 - Sharp turning

DriveSafe was coded using Android studio which is based on Java and XML. In DriveSafe:
 - Accelerometer sensor is used to measure acceleration in 3D space. 
 - Haversine formula is applied to calculate distances and speed from GPS coordinates. 
 - The data collected and stored for later analysis.
 - The user interface was integrated with Google Maps API to provide the user with an interactive experience.

## Data collection and Testing: <a name = "data-collection-and-testing"></a>
The app was run on Bangalore roads over 2 months. The acceleration, the speed, the time and the location of the vehicle was recorded every second.
Using the data collected, I set appropriate thresholds for the rash driving behaviours. DriveSafe was also tested in different road conditions to test its reliability and accuracy.

DriveSafe can be used to monitor reckless driving behaviours with a high accuracy and improve road safety by individuals, fleet operators, insurance companies, and traffic police.

## Explainer video: <a name = "explainer-video"></a>

<p align = "center">
    <a href = "https://youtu.be/BaOvaS84id0">
        <img src = "https://img.youtube.com/vi/BaOvaS84id0/0.jpg">
    </a>
</p>
