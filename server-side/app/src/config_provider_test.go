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

func withAfterCloneStateFile(t *testing.T, fn func(stateFile string)) {
	t.Helper()

	prev := afterCloneStateFile
	afterCloneStateFile = filepath.Join(t.TempDir(), "after_clone_state.json")
	t.Cleanup(func() {
		afterCloneStateFile = prev
	})

	fn(afterCloneStateFile)
}

func TestRunAfterCloneIfNeededTracksStatePerContainer(t *testing.T) {
	withAfterCloneStateFile(t, func(stateFile string) {
		dir := t.TempDir()
		marker := "after_clone_ran.txt"
		cmd := "touch " + marker

		if err := runAfterCloneIfNeeded(dir, cmd); err != nil {
			t.Fatal(err)
		}
		if _, err := os.Stat(filepath.Join(dir, marker)); err != nil {
			t.Fatalf("expected after-clone command to run, got: %v", err)
		}

		if err := os.Remove(filepath.Join(dir, marker)); err != nil {
			t.Fatal(err)
		}

		if err := runAfterCloneIfNeeded(dir, cmd); err != nil {
			t.Fatal(err)
		}
		if _, err := os.Stat(filepath.Join(dir, marker)); !os.IsNotExist(err) {
			t.Fatalf("expected after-clone command to be skipped when already executed in this container")
		}

		stateBytes, err := os.ReadFile(stateFile)
		if err != nil {
			t.Fatal(err)
		}
		if len(stateBytes) == 0 {
			t.Fatal("expected after-clone state to be persisted")
		}
	})
}

func TestRunAfterCloneIfNeededRunsWhenCommandChanges(t *testing.T) {
	withAfterCloneStateFile(t, func(stateFile string) {
		dir := t.TempDir()
		firstMarker := "first.txt"
		secondMarker := "second.txt"

		if err := runAfterCloneIfNeeded(dir, "touch "+firstMarker); err != nil {
			t.Fatal(err)
		}
		if err := runAfterCloneIfNeeded(dir, "touch "+secondMarker); err != nil {
			t.Fatal(err)
		}

		if _, err := os.Stat(filepath.Join(dir, firstMarker)); err != nil {
			t.Fatalf("expected first marker, got: %v", err)
		}
		if _, err := os.Stat(filepath.Join(dir, secondMarker)); err != nil {
			t.Fatalf("expected second marker after command change, got: %v", err)
		}

		state, err := loadAfterCloneState()
		if err != nil {
			t.Fatal(err)
		}
		if state.Executed[dir] != "touch "+secondMarker {
			t.Fatalf("expected state to track latest command, got: %q", state.Executed[dir])
		}
	})
}
