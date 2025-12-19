# Monte Carlo π Estimation Project

### Prerequisites

- **Java 21** or higher
- **Maven 3.6+**
- JavaFX 21 (automatically downloaded by Maven)

### Building the Project

```bash
# Navigate to project directory
cd Test

# Clean and compile
mvn clean compile

# Package (optional)
mvn package
```

### Running the Application

#### Option 1: Run GUI Application

```bash
mvn javafx:run
```


##  Project Structure

```
Test/
├── pom.xml                        # Maven configuration
├── README.md                      # This file
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── edu/montecarlo/
│   │   │       ├── model/         # Core estimation logic
│   │   │       ├── experiment/    # Experiment framework
│   │   │       ├── gui/           # JavaFX GUI components
│   │   │       └── PiEstimationApp.java
│   │   └── resources/
│   │       └── edu/montecarlo/gui/
│   │           ├── main.fxml      # GUI layout
│   │           └── style.css      # Styling
```

