package main

import (
	"encoding/json"
	"fmt"
	"os"
	"strings"
)

const (
	CONFIG_FILE = "/app/config/config.json"
	REPOS_DIR   = "/app/repo-store"
)

type ProjectConfig struct {
	Name          string `json:"name"`
	Url           string `json:"repo_url"`
	CmdAfterClone string `json:"repo_cmd_after_clone,omitempty"`
}

func loadConfigurations() ([]*ProjectConfig, error) {
	if _, e := os.Stat(CONFIG_FILE); os.IsNotExist(e) {
		return make([]*ProjectConfig, 0), nil
	}
	jBytes, e := os.ReadFile(CONFIG_FILE)

	if e != nil {
		return nil, e
	}

	var configs []*ProjectConfig
	if e = json.Unmarshal(jBytes, &configs); e != nil {
		return nil, e
	}

	return configs, nil
}

func toConfiguration(p *ProjectConfig) *Configuration {
	pathParts := strings.Split(p.Url, "/")
	repoName := strings.TrimRight(pathParts[len(pathParts)-1], ".git")

	return &Configuration{
		id:            repoName,
		repoDir:       REPOS_DIR + "/" + repoName,
		repoUrl:       p.Url,
		cmdAfterClone: p.CmdAfterClone,
	}
}

func runCmdInRepo(repoDir, cmd string) error {
	if cmd == "" {
		return nil
	}

	fmt.Printf("Running after-clone command in '%s': %s\n", repoDir, cmd)
	shell := &Shell{Cwd: repoDir}
	out, err := shell.Execute(cmd)
	if err != nil {
		return fmt.Errorf("%w\nOutput: %s", err, out)
	}
	return nil
}
