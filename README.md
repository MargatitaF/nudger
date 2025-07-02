# 💰 Nudger

A minimal, low-friction mobile app designed to help you reduce habitual spending through personalized reminder notifications.

## 🎯 Purpose

Nudger helps people with regular, predictable spending habits stay mindful of their financial goals. Instead of complex budgeting tools, Nudger takes a "set it and forget it" approach - configure your preferences once, and receive gentle reminders that keep your spending awareness front of mind.

## ✨ Key Features

### 📱 Smart Notifications
- **Scheduled Reminders**: Set up custom notification schedules to remind yourself of your financial goals
- **Personalized Tones**: Choose from four distinct notification styles:
  - 🤗 **Caring**: Gentle, supportive reminders
  - 💪 **Assertive**: Direct, firm nudges
  - 📝 **Neutral**: Straightforward, factual messages
  - 🌟 **Encouraging**: Motivational, positive reinforcement

### ⚡ Ultra Low-Friction Experience
- Set up once, benefit continuously
- No complex budget tracking or transaction linking required
- Minimal configuration for maximum impact
- Perfect for people who want spending awareness without the overhead

### 🎨 Clean, Modern Interface
- Intuitive Android app built with Kotlin and Jetpack Compose
- Simple notification preference management
- Focus on usability over feature bloat

## 🏗️ Architecture

Nudger consists of two main components:

### 📱 Android Frontend (`/ui`)
- **Technology**: Kotlin, Jetpack Compose, Material Design 3
- **Features**: 
  - Notification preference configuration
  - Tone selection and customization
  - Clean, modern UI following Material Design principles

### 🔧 Backend API (`/src`)
- **Technology**: Python, FastAPI, SQLite, Alembic
- **Features**:
  - Notification scheduling and delivery
  - User preference management
  - RESTful API for mobile app communication
  - Database migrations with Alembic

## 🚀 Getting Started

### Prerequisites
- **Android Development**: Android Studio, Kotlin
- **Backend Development**: Python 3.8+, UV package manager

### Backend Setup
```bash
# ========== First time setup ==========
# Navigate to project root
cd nudger

# Install dependencies with UV
uv sync

# Run database migrations
cd src
alembic upgrade head
# =======================================
# Start the API server
uv run api.py
```

### Android App Setup

#### Option 1: Using Android Studio (Recommended)
1. Open Android Studio
2. Open the `ui` project folder
3. Wait for Gradle sync to complete
4. Click the "Run" button or press `Ctrl+R` to run on an emulator or connected device

#### Option 2: Command Line Build
```bash
# Navigate to UI directory
cd ui

# Build and run the app
./gradlew assembleDebug
./gradlew installDebug
```

**Note**: For visitors/reviewers who want to try the app, using Android Studio with an emulator is the easiest option without needing to install anything on a physical device.

## 📋 Development Status

**Current Status**: MVP (Minimum Viable Product)

### ✅ Completed Features
- Core notification scheduling system
- Android app with preference management
- Multiple notification tone options
- FastAPI backend with database integration

### 🔮 Future Roadmap
- **Cloud Database Integration**: Add support for a cloud database such as Supabase for scalable, cross-device data storage
- **Automatic Backend Hosting**: Host the backend API in the cloud or automate its startup, so the app works without manual server setup
- **Transaction Analysis**: Integrate with banking APIs to analyze spending patterns
- **Smart Notifications**: AI-driven notifications based on actual spending behavior
- **Advanced Scheduling**: Context-aware notification timing
- **Analytics Dashboard**: Spending awareness metrics and insights

## 🎯 Target Audience

Nudger is designed for:
- Individuals with regular, predictable spending habits
- People who want spending awareness without complex budgeting tools
- Users seeking a "set and forget" approach to financial mindfulness
- Anyone looking to break habitual spending patterns with gentle reminders

## 🛠️ Technical Stack

### Frontend
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Design System**: Material Design 3
- **Architecture**: MVVM with ViewModels

### Backend
- **Language**: Python
- **Framework**: FastAPI
- **Database**: PostgreSQL + SQLite with Alembic migrations
- **Notifications**: Custom scheduling system
- **Package Management**: UV

## 🤝 Contributing

This is currently an MVP project. Contributions and feedback are welcome!

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## 🔗 Contact

**Email**: 
- 557795@student.fontys.nl (Fontys email)
- ritafedulova@hotmail.com (Personal email)
- Margarita Fedulova (Teams)

---

*Nudger: Mindful Spending. One nudge at a time* 💡
