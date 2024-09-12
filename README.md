# Weather App

A simple weather application that fetches current weather information using the OpenWeatherMap API and displays it in a user-friendly interface.
Built with Kotlin for Android, it utilizes Retrofit for networking and Gson for JSON parsing.

## Features

- Get real-time weather data for any location.
- Display temperature, humidity, wind speed, sunrise, sunset times, and weather description.
- Supports metric (Celsius) temperature format.
- Displays weather icons based on current conditions.
- Caches the last weather response locally using SharedPreferences.
- Retry fetching weather data with a refresh button.

## Tech Stack

- **Kotlin** - Main programming language used for the project.
- **Retrofit** - For making API requests to OpenWeatherMap.
- **Gson** - For parsing JSON responses.
- **OpenWeatherMap API** - Used to fetch weather data.
- **SharedPreferences** - To cache and store the last fetched weather data.

## Prerequisites

Before you begin, ensure you have met the following requirements:

- Android Studio installed (minimum version X.X.X).
- An OpenWeatherMap API key.
- Android device or emulator running Android version 7.0 (API Level 24) or higher.

## License
- This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing
- Contributions are always welcome! Please open an issue or submit a pull request if you find any bugs or want to add new features.
