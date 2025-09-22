package main

import (
	"fmt"
	"net/http"
	"os"
)

func main() {
	alias := os.Getenv("ALIAS")
	if alias == "" {
		 alias = "undefined"
	}

	healthApi := &HealthHttpApi{
		Alias: alias,
	}
	http.HandleFunc("/api/health", healthApi.getHealth)

	configs, e := loadConfigurations()
	if e != nil {
		panic(e)
	}

	if len(configs) == 0 {
		panic("No configurations could be loaded from '" + CONFIG_FILE + "'")
	}

	for _, c := range configs {
		p := NewHttpProject(toConfiguration(c))
		p.Start()
	}

	projectApi := &ProjectApi{
		Configs: configs,
	}
	projectApi.Start()

	fmt.Println("Starting server...")
	http.ListenAndServe(":80", nil)
}

type HealthHttpApi struct {
	Alias        string
}

func (h *HealthHttpApi) getHealth(w http.ResponseWriter, req *http.Request) {
	fmt.Fprintf(w, "{ \"alias\": \"%s\", \"status\": \"OK!\" }", h.Alias)
}
