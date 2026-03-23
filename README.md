# ActaCofrade

ActaCofrade is a web application designed to manage events, tasks, and decisions for brotherhoods (cofradías). It helps organize activities efficiently and keeps track of all important information.

The project is divided into two main parts: a **Spring Boot** backend and an **Angular** frontend.

## Folder Structure

* `/backend`: Spring Boot REST API (Java 17).
* `/frontend`: Angular Single Page Application.
* `/docs`: Initial project documentation and proposals.
* `docker-compose.yml`: Docker configuration to easily run the database and the backend.

## Prerequisites

Before you start, make sure you have installed:
* [Docker Desktop](https://www.docker.com/products/docker-desktop/) (to run the database and backend easily).
* [Node.js](https://nodejs.org/) (to run the Angular frontend).
* Git.

## How to run the project locally

### 1. Setup Environment Variables
First, you need to set up the environment variables. Copy the `.env.example` file and rename it to `.env`:
```bash
cp .env.example .env
```
*(Note: Do not commit the `.env` file, it is already ignored in the `.gitignore`).*

### 2. Start Database and Backend
Open a terminal in the root folder of the project and run Docker Compose:
```bash
docker compose up -d --build
```
This command will download the necessary images and start:
* A **PostgreSQL database** running on port `5432`.
* The **Spring Boot API** running on port `8080`.

### 3. Start Frontend
Open a new terminal, go into the `frontend` folder, install the dependencies, and start the development server:
```bash
cd frontend
npm install
npm start
```
Now, you can open your browser and go to `http://localhost:4200` to see the application.

## Collaborative Work
* Make sure your code works locally before creating a commit.
* Write clear and simple commit messages describing what you did.
* Do not expose real passwords or secret keys in your code.
