package main

import (
	"encoding/json"
	"fmt"
	"os"
	"strings"
)

const (
	CONFIG_FILE            = "/app/config/config.json"
	REPOS_DIR              = "/app/repo-store"
	LOCAL_STATE_DIR        = "/app/local-state"
	AFTER_CLONE_STATE_FILE = LOCAL_STATE_DIR + "/after_clone_state.json"
)

var afterCloneStateFile = AFTER_CLONE_STATE_FILE

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

type afterCloneState struct {
	Executed map[string]string `json:"executed"`
}

func loadAfterCloneState() (*afterCloneState, error) {
	state := &afterCloneState{Executed: make(map[string]string)}
	if _, e := os.Stat(afterCloneStateFile); os.IsNotExist(e) {
		return state, nil
	}

	jBytes, e := os.ReadFile(afterCloneStateFile)
	if e != nil {
		return nil, e
	}

	if len(jBytes) == 0 {
		return state, nil
	}

	if e = json.Unmarshal(jBytes, state); e != nil {
		return nil, e
	}
	if state.Executed == nil {
		state.Executed = make(map[string]string)
	}

	return state, nil
}

func saveAfterCloneState(state *afterCloneState) error {
	if e := os.MkdirAll(LOCAL_STATE_DIR, 0755); e != nil {
		return e
	}

	jBytes, e := json.Marshal(state)
	if e != nil {
		return e
	}

	return os.WriteFile(afterCloneStateFile, jBytes, 0644)
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

func runAfterCloneIfNeeded(repoDir, cmd string) error {
	if cmd == "" {
		return nil
	}

	state, e := loadAfterCloneState()
	if e != nil {
		return e
	}

	if state.Executed[repoDir] == cmd {
		fmt.Printf("After-clone command already executed in this container for '%s': %s\n", repoDir, cmd)
		return nil
	}

	if err := runCmdInRepo(repoDir, cmd); err != nil {
		return err
	}

	state.Executed[repoDir] = cmd
	return saveAfterCloneState(state)
}
