# ---------- Railway backend image (plain Java, no Spring Boot) ----------
# Builds and runs ONLY the Java backend/API. The frontend is deployed on Vercel.
FROM eclipse-temurin:21-jdk

WORKDIR /app

# 1) Copy the Java source code
COPY backend/src ./backend/src

# 2) Compile every .java file into ./out
RUN mkdir -p out && javac -d out $(find backend/src -name "*.java")

# 3) Railway injects the PORT variable at runtime; Main.java reads it.
ENV PORT=8080
EXPOSE 8080

# 4) Start Main.java
CMD ["java", "-cp", "out", "com.ejra.smiles.Main"]
