package main

import (
	"os"
	"path/filepath"
	"testing"
)

func TestRunCmdInRepo(t *testing.T) {
	dir := t.TempDir()
	marker := "marker.txt"

	if err := runCmdInRepo(dir, "touch "+marker); err != nil {
		t.Fatal(err)
	}

	if _, err := os.Stat(filepath.Join(dir, marker)); err != nil {
		t.Fatalf("expected command to run in repo root, got: %v", err)
	}

	if err := runCmdInRepo(dir, ""); err != nil {
		t.Fatalf("empty command should be a no-op, got: %v", err)
	}
}
