package main

import (
	"encoding/json"
	"errors"
	"os"
	"strings"
)

const (
	CONFIG_FILE   = "/app/config/config.json"
	REPO_LINK_VAR = "APP_REPO_LINK"
	REPOS_DIR     = "/app/repo-store"
)

type ProjectConfig struct {
	Url string `json:"repo_url"`
}

func loadConfigurations() ([]*Configuration, error) {
	if _, e := os.Stat(CONFIG_FILE); os.IsNotExist(e) {
		return loadDefaultConfiguration()
	}

	jBytes, e := os.ReadFile(CONFIG_FILE)

	if e != nil {
		return nil, e
	}

	var configs []*ProjectConfig
	if e = json.Unmarshal(jBytes, &configs); e != nil {
		return nil, e
	}

	results := make([]*Configuration, 0)

	for _, config := range configs {
		pathParts := strings.Split(config.Url, "/")
		repoName := pathParts[len(pathParts)-1]

		results = append(results, &Configuration{
			id:      strings.TrimRight(repoName, ".git"),
			repoDir: REPOS_DIR + "/" + repoName,
			repoUrl: config.Url,
		})
	}

	return results, nil
}

func loadDefaultConfiguration() ([]*Configuration, error) {
	repoLink := os.Getenv(REPO_LINK_VAR)

	if repoLink == "" {
		return nil, errors.New("env.variable not defined: " + REPO_LINK_VAR + ". Pass repo link for cloning")
	}

	result := &Configuration{
		repoDir: CWD,
		repoUrl: repoLink,
	}

	results := make([]*Configuration, 0)
	return append(results, result), nil
}
