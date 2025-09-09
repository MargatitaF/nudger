package main

import (
	"fmt"
	"net/http"
)

type Mood struct {
	Mood   string
	Prompt string
}

func main() {

	// Initialize moods
	moods := []Mood{
		{"caring", "Test prompt 1."},
		{"neutral", "Test prompt 2."},
	}

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		fmt.Println("Hello, World!")
	})

	/* Endpoint to list moods */
	http.HandleFunc("/moods", func(w http.ResponseWriter, r *http.Request) {
		for _, mood := range moods {
			fmt.Printf("Mood: %s, Prompt: %s\n", mood.Mood, mood.Prompt)
		}
	})
	if err := http.ListenAndServe(":8080", nil); err != nil {
		fmt.Println("Server error:", err)
	}
}
