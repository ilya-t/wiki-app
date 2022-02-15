package main

import (
	"os"
	"testing"
)

func TestNothingOutdatedAtPlainStructure(t *testing.T) {
	reference := NewFile("readme.md", "11", nil)
	results, e := ShowOutdated(reference, reference)

	if e != nil {
		t.Error(e)
	}

	if results != nil {
		t.Error("Expecting no outdated", results)
	}

}

func TestSomethingOutdatedAtPlainStructure(t *testing.T) {
	reference := NewFile("readme.md", "ref_hash", nil)
	subject := NewFile("readme.md", "subj_hash", nil)
	results, e := ShowOutdated(reference, subject)

	if e != nil {
		t.Error(e)
	}

	if results.hash != "ref_hash" {
		t.Error("Wrong outdated result", results)
	}

}

func TestSomethingOutdatedAtNestedStructure(t *testing.T) {
	reference := NewFile(".", "12", []*File{
		&File{
			name:  "up_to_date_file",
			hash:  "uptodate",
			files: nil,
		},
		&File{
			name:  "outdated_file",
			hash:  "outdated_hash",
			files: nil},
	},
	)
	subject := NewFile(".", "11", []*File{
		&File{
			name:  "up_to_date_file",
			hash:  "uptodate",
			files: nil,
		},
		&File{
			name:  "outdated_file",
			hash:  "119",
			files: nil},
	},
	)
	results, e := ShowOutdated(reference, subject)

	if e != nil {
		t.Error(e)
	}

	if len(results.files) != 1 {
		t.Error("Expecting only one outdated file", results.files)
	}
	outdated := results.files[0]

	if outdated.hash != "outdated_hash" {
		t.Error("Wrong outdated result", outdated)
	}
}

func TestSomethingNewAtNestedStructure(t *testing.T) {
	reference := NewFile(".", "12", []*File{
		&File{
			name:  "up_to_date_file",
			hash:  "uptodate",
			files: nil,
		},
		&File{
			name:  "new_file",
			hash:  "new_file",
			files: nil},
	},
	)
	subject := NewFile(".", "11", []*File{
		&File{
			name:  "up_to_date_file",
			hash:  "uptodate",
			files: nil,
		},
	},
	)
	results, e := ShowOutdated(reference, subject)

	if e != nil {
		t.Error(e)
	}

	if len(results.files) != 1 {
		t.Error("Expecting only one outdated file", results.files)
	}
	new_file := results.files[0]

	if new_file.hash != "new_file" {
		t.Error("Wrong outdated result", new_file)
	}
}

func TestSomethingOutdatedAtDeeplyNestedStructure(t *testing.T) {
	reference := NewFile(".", "12", []*File{
		&File{
			name:  "up_to_date_file",
			hash:  "uptodate",
			files: nil,
		},
		&File{
			name: "partly_outdated_dir",
			hash: "partly_outdated_dir",
			files: []*File{
				&File{
					name:  "up_to_date_file2",
					hash:  "uptodate2",
					files: nil,
				},
				&File{
					name:  "outdated_file",
					hash:  "outdated_file",
					files: nil,
				},
			}},
	},
	)
	subject := NewFile(".", "11", []*File{
		&File{
			name:  "up_to_date_file",
			hash:  "uptodate",
			files: nil,
		},
		&File{
			name: "partly_outdated_dir",
			hash: "__",
			files: []*File{
				&File{
					name:  "up_to_date_file2",
					hash:  "uptodate2",
					files: nil,
				},
				&File{
					name:  "outdated_file",
					hash:  "__",
					files: nil,
				},
			}},
	},
	)
	results, e := ShowOutdated(reference, subject)

	if e != nil {
		t.Error(e)
	}

	partly_outdated_dir := results.files[0]

	if len(partly_outdated_dir.files) != 1 {
		t.Error("Expecting only one outdated file. Got: ", partly_outdated_dir)
	}
	outdated_file := partly_outdated_dir.files[0]

	if outdated_file.hash != "outdated_file" {
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

	if len(result.files) != 2 {
		t.Error("Expecting 2 files. Got: ", result.files)
	}

	if result.files[0].name != "README.md" {
		t.Error("Expecting 'README.md' file. Got: ", result.files[0])
	}

	note_file := result.files[1].files[0]
	if note_file.name != "note.md" {
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
	}

	if len(result.files) != 1 {
		t.Error("Expecting 1 files. Got: ", result.files)
	}

	if result.files[0].name != "README.md" {
		t.Error("Expecting 'README.md' file. Got: ", result.files[0])
	}
}
