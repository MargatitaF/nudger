package main

import (
	"fmt"
	"log"
	"os"

	"github.com/joho/godotenv"
)

func main() {
	// Load environment variables from .env file
	err := godotenv.Load(".env")
	if err != nil {
		log.Fatalf("Error loading .env file")
	}

	// Example usage of an environment variable
	environment := os.Getenv("ENVIRONMENT")
	version := os.Getenv("VERSION")
	fmt.Printf("Environment: %s, Version: %s\n", environment, version)

	DB_USER := os.Getenv("DB_USER")
	DB_PASSWORD := os.Getenv("DB_PASSWORD")
	DB_HOST := os.Getenv("DB_HOST")
	DB_PORT := os.Getenv("DB_PORT")
	DB_NAME := os.Getenv("DB_NAME")

	if DB_USER == "" || DB_PASSWORD == "" || DB_HOST == "" || DB_PORT == "" || DB_NAME == "" {
		log.Fatal("Database environment variables are not set")
}