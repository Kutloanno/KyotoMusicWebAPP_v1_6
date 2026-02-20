# ==========================================
# STAGE 1: THE BUILDER
# ==========================================
# 1. Grab a virtual computer that already has Maven and Java installed. Name this stage "build".
FROM maven:3.9.6-eclipse-temurin-21 AS build

# 2. Create a folder inside this virtual computer called /app and step inside it.
WORKDIR /app

# 3. Copy your project's pom.xml from your laptop into this virtual computer.
COPY pom.xml .

# 4. Copy all your source code (the src folder) into the virtual computer.
COPY src ./src

# 5. Tell Maven to build your app into a .jar file (just like clicking "Build" in IntelliJ).
# We skip tests here to make the cloud deployment faster.
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: THE RUNNER
# ==========================================
# 6. Now, grab a brand new, lightweight virtual computer that ONLY has Java (no Maven needed anymore).
FROM eclipse-temurin:21-jre
WORKDIR /app

# 7. Reach back into the "build" computer, grab the finished .jar file, and put it in this new computer.
COPY --from=build /app/target/*.jar app.jar

# 8. Tell the virtual computer to open port 8080 so internet traffic can get in.
EXPOSE 8080

# 9. The final command: Run the app!
ENTRYPOINT ["java", "-jar", "app.jar"]