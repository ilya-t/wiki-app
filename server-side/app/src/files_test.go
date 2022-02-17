package main

import (
	"encoding/json"
	"os"
	"testing"
)

func TestNothingOutdatedAtPlainStructure(t *testing.T) {
	reference := NewFile("readme.md", "11")
	results, e := ShowOutdated(reference, reference)

	if e != nil {
		t.Error(e)
	}

	if results != nil {
		t.Error("Expecting no outdated", results)
	}

}

func TestSomethingOutdatedAtPlainStructure(t *testing.T) {
	reference := NewFile("readme.md", "ref_hash")
	subject := NewFile("readme.md", "subj_hash")
	results, e := ShowOutdated(reference, subject)

	if e != nil {
		t.Error(e)
	}

	if results.Hash != "ref_hash" {
		t.Error("Wrong outdated result", results)
	}

}

func TestSomethingOutdatedAtNestedStructure(t *testing.T) {
	reference := NewDir(".", "12", []*File{
		NewFile("up_to_date_file", "uptodate"),
		NewFile("outdated_file", "outdated_hash"),
	},
	)
	subject := NewDir(".", "11", []*File{
		NewFile("up_to_date_file", "uptodate"),
		NewFile("outdated_file", "119"),
	},
	)
	results, e := ShowOutdated(reference, subject)

	if e != nil {
		t.Error(e)
	}

	if len(results.Files) != 1 {
		t.Error("Expecting only one outdated file", results.Files)
	}
	outdated := results.Files[0]

	if outdated.Hash != "outdated_hash" {
		t.Error("Wrong outdated result", outdated)
	}
}

func TestSomethingNewAtNestedStructure(t *testing.T) {
	reference := NewDir(".", "12", []*File{
		NewFile("up_to_date_file", "uptodate"),
		NewFile("new_file", "new_file"),
	},
	)
	subject := NewDir(".", "11", []*File{
		NewFile("up_to_date_file", "uptodate"),
	},
	)
	results, e := ShowOutdated(reference, subject)

	if e != nil {
		t.Error(e)
	}

	if len(results.Files) != 1 {
		t.Error("Expecting only one outdated file", results.Files)
	}
	new_file := results.Files[0]

	if new_file.Hash != "new_file" {
		t.Error("Wrong outdated result", new_file)
	}
}

func TestSomethingOutdatedAtDeeplyNestedStructure(t *testing.T) {
	reference := NewDir(".", "12", []*File{
		NewFile("up_to_date_file", "uptodate"),
		NewDir("partly_outdated_dir", "partly_outdated_dir", []*File{
			NewFile("up_to_date_file2", "uptodate2"),
			NewFile("outdated_file", "outdated_file"),
		}),
	},
	)
	subject := NewDir(".", "11", []*File{
		NewFile("up_to_date_file", "uptodate"),
		NewDir("partly_outdated_dir", "__", []*File{
			NewFile("up_to_date_file2", "uptodate2"),
			NewFile("outdated_file", "__"),
		}),
	})
	results, e := ShowOutdated(reference, subject)

	if e != nil {
		t.Error(e)
	}

	partly_outdated_dir := results.Files[0]

	if len(partly_outdated_dir.Files) != 1 {
		t.Error("Expecting only one outdated file. Got: ", partly_outdated_dir)
	}
	outdated_file := partly_outdated_dir.Files[0]

	if outdated_file.Hash != "outdated_file" {
		t.Error("Wrong outdated result", outdated_file)
	}
}

func createEmptyFile(path string, name string, t *testing.T) {
	err := os.MkdirAll(path, 0755)

	if err != nil {
		t.Error(err)
	}

	d := []byte("")
	e := os.WriteFile(path+"/"+name, d, 0644)
	if e != nil {
		t.Error(e)
	}
}

func TestFileStructureGeneration(t *testing.T) {
	path := "/tmp/gen_test"
	os.RemoveAll(path)
	createEmptyFile(path, "README.md", t)
	createEmptyFile(path+"/dir", "note.md", t)
	result, e := GenerateStructure(path)
	if e != nil {
		t.Error(e)
	}

	if len(result.Files) != 2 {
		t.Error("Expecting 2 files. Got: ", result.Files)
	}

	if result.Files[0].Name != "README.md" {
		t.Error("Expecting 'README.md' file. Got: ", result.Files[0])
	}

	note_file := result.Files[1].Files[0]
	if note_file.Name != "note.md" {
		t.Error("Expecting 'note.md' file. Got: ", note_file)
	}
}

func TestFileStructureGenerationSkipsGit(t *testing.T) {
	path := "/tmp/gen_test"
	os.RemoveAll(path)
	createEmptyFile(path, "README.md", t)
	createEmptyFile(path+"/.git", "HEAD", t)
	result, e := GenerateStructure(path)

	if e != nil {
		t.Error(e)
		return
	}

	if len(result.Files) != 1 {
		t.Error("Expecting 1 files. Got: ", result.Files)
		return
	}

	if result.Files[0].Name != "README.md" {
		t.Error("Expecting 'README.md' file. Got: ", result.Files[0])
		return
	}
}

func TestHashAlgorithm(t *testing.T) {
	content := []byte("do not modify this file! is's sha1 is used under tests \n")
	path := "/tmp/file_with_fixed_sha1"
	err := os.WriteFile(path, content, 0644)

	if err != nil {
		t.Error(err)
		return
	}

	h, e := hashOfFile(path)

	if e != nil {
		t.Error(e)
	}

	expected := "0758fe8844f102aaa616c30c94ea4f8eb9326b06"
	if h != expected {
		t.Error("Expecting '" + expected + "', Got: " + h)
	}
}

func TestParsingFilesJson(t *testing.T) {
	jsonString := "[{\"name\": \"file_name\", \"hash\": \"some_hash\"}]"

	var files []*File
	if err := json.Unmarshal([]byte(jsonString), &files); err != nil {
		t.Error(err)
		return
	}

	actual := files[0]
	expected := NewFile("file_name", "some_hash")

	if expected.Name != actual.Name || expected.Hash != actual.Hash {
		t.Errorf("Expected: %+v\nGot: %+v\n", expected, actual)
	}
}
