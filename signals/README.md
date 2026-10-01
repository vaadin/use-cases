# Vaadin Signal API Use Cases

This repository contains a collection of Vaadin views demonstrating the Signal API for reactive state management.

## Overview

The Vaadin Signal API provides reactive state management for Vaadin Flow applications. This project contains **30 implemented use cases** (23 single-user + 7 multi-user) showcasing various signal patterns and real-world UI scenarios.

## Use Cases

### Single-User Use Cases

1. **Dynamic Button State** - Form validation with reactive button enable/disable
2. **Progressive Disclosure** - Nested conditional form sections
3. **Interactive SVG Shape Editor** - Dynamic SVG manipulation with extensive attribute binding
4. **Filtered Data Grid** - Client-side filtering and sorting
5. **Cascading Selector** - Dependent dropdown menus (country → state → city)
6. **Shopping Cart** - Real-time cart totals and item management
8. **Multi-Step Wizard** - Form wizard with validation and navigation
9. **Form with Binder Integration** - Vaadin Binder with signal-based validation
11. **Responsive Layout** - Window size as signal for responsive UI
13. **Current User Signal** - Application-wide user context from Spring Security
14. **Async Data Loading** - Loading/success/error states for async operations
15. **Debounced Search** - Search-as-you-type with 300ms debouncing
16. **URL State Integration** - Query parameters as signals with router integration
17. **Custom PC Builder** - Complex state at scale with ~70 interdependent signals
18. **LLM-Powered Task Management** - AI assistant with real-time chat interface
19. **Parallel Data Loading** - Per-item loading states with individual spinners
20. **User Preferences** - Session-scoped signal for user settings
21. **Signals-Based i18n** - Reactive internationalization with ResourceBundles
22. **Two-Way Mapped Signals** - Bidirectional field-to-record binding
23. **Real-time Dashboard** - Interactive dashboard with charts and live metrics
24. **VirtualList Notifications** - Notification inbox with VirtualList bound to signal data source

### Multi-User Collaboration (7 total)

1. **Shared Chat** - Collaborative message list with append-only operations
2. **Collaborative Cursors** - Real-time cursor position sharing
3. **Click Race Game** - Conflict resolution and atomic operations
4. **Collaborative Editing** - Field-level locking for concurrent form editing
6. **Shared Task List** - Real-time collaborative task management with inline editing
7. **Shared LLM Task List** - Multi-user LLM-powered task management with chat
8. **Broadcast Announcement** - Post an announcement that appears instantly in every open session (banner + notification)

## Project Structure

```
src/main/java/com/example/
├── usecase01/                       # UC01-UC23 (one package per use case)
│   └── UseCase01View.java
├── usecase02/ ... usecase23/
├── muc01/                           # MUC01-MUC08 (one package per use case)
│   ├── MUC01View.java
│   └── MUC01Signals.java           # Per-MUC shared signal class
├── muc02/ ... muc08/
├── security/
│   ├── CurrentUserSignal.java       # Application-scoped user context signal
│   ├── SecurityConfiguration.java   # Spring Security setup
│   └── SecurityService.java
├── service/                         # Shared services (analytics, data loading)
├── signals/
│   ├── UserSessionRegistry.java     # Active user tracking
│   ├── UserInfo.java
│   └── SessionIdHelper.java
├── preferences/
│   └── UserPreferences.java         # Session-scoped user preferences
├── listeners/
│   └── UserInteractionTracker.java
├── views/
│   ├── MainLayout.java              # App layout with navigation
│   ├── LoginView.java               # Login page
│   ├── HomeView.java                # Landing page
│   └── ActiveUsersDisplay.java
└── MissingAPI.java                  # Helper methods for signal bindings
```

## Running the Application

1. **Prerequisites**: Java 25+, Maven 3.9+
2. **Build**: `mvn clean install`
3. **Run**: `mvn spring-boot:run`
4. **Access**: Open http://localhost:8080

### Login Credentials

- `viewer` / `password` (VIEWER role)
- `editor` / `password` (EDITOR role)
- `admin` / `password` (ADMIN role)
- `superadmin` / `password` (SUPER_ADMIN role)

## Key Patterns Demonstrated

### Basic Reactivity
- One-way and two-way bindings
- Computed/derived signals
- Signal transformations with `map()`

### UI Patterns
- Conditional rendering (`bindVisible`, `bindEnabled`)
- Dynamic text content (`bindText`, `bindElementText`)
- Responsive layouts (browser window size as signal)
- Form validation with signals

### Advanced Patterns
- Vaadin Binder integration with signals
- Application-scoped signals (Spring @Component)
- Multi-user collaboration with shared signals
- Browser integration (window size, page title)
- Spring Security integration

## MissingAPI Helpers

Since the official Signal API is still in development, `MissingAPI.java` provides temporary helper methods for bindings not yet in the framework:

- `bindItems()` - List binding for Grid/ComboBox (multiple overloads for Signal, ListSignal, SharedListSignal)
- `bindBrowserTitle()` - Browser document title binding
- `bindInvalid()` - Component invalid state binding
- `tabsSyncSelectedIndex()` - Bidirectional Tabs selected index synchronization

## Multi-User Architecture

Multi-user use cases (MUC 01-04, 06-08) demonstrate collaborative features using:

- **Per-MUC Signal Classes** - Each MUC has its own application-scoped Spring component (MUC01Signals, MUC02Signals, MUC03Signals, MUC04Signals, MUC06Signals, MUC07Signals, MUC08Signals) holding shared signals
- **UserSessionRegistry** - Tracks active users with reactive signal
- **onAttach/onDetach** - Lifecycle hooks for user registration

All users see real-time updates via Vaadin's automatic UI synchronization.

## Documentation

- **signal-use-cases.md** - Detailed descriptions and patterns for all 30 use cases

## Technical Stack

- **Vaadin 25.2-SNAPSHOT** - Web framework with Signal API
- **Spring Boot 4.0.1** - Application framework
- **Spring Security** - Authentication and authorization
- **Java 25** - Language and platform
- **Maven** - Build tool

## Related Resources

- [Vaadin Signal API Proposal](https://github.com/vaadin/platform/issues/8366)
- [Vaadin Flow Documentation](https://vaadin.com/docs/latest/flow)
- [Spring Security with Vaadin](https://vaadin.com/docs/latest/flow/security)

---

**Status**: Active development
**Last Updated**: 2026-02-16
**Total Use Cases**: 30 (23 single-user + 7 multi-user)
