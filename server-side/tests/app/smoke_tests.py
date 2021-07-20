import subprocess
import unittest

import requests
import backend
import zipfile
import os

HOST = 'http://test-backend'
LATEST = HOST + '/api/1/revision/latest'


class ChatAcceptanceTests(unittest.TestCase):
    def setUp(self) -> None:
        super().setUp()
        backend.wait_boot()

    def test_latest_revision_zip_file(self):
        response = requests.get(LATEST)
        if response.headers["Content-Type"] != "application/zip":
            self.fail(msg='Wrong content received: ' + response.text)
            return

        content_disposition = response.headers["Content-Disposition"]
        file_name = content_disposition[content_disposition.index("=") + 1:]
        head_commit_hash = os.environ['HEAD_COMMIT']
        self.assertNotEqual("", head_commit_hash)
        self.assertEqual(head_commit_hash + ".zip", file_name,
                         msg='Expected "'+head_commit_hash+'" payload, but got: ' + file_name)

    def test_latest_revision_zip_content_not_contains_git(self):
        def git_folder_excluded(dir):
            if os.path.exists(dir + "/.git"):
                return ".git included to snapshot!"
            return None

        self.assertRevisionContains(condition=git_folder_excluded)

    def test_latest_revision_zip_content_contains_root_content(self):
        def root_contains_concrete_readme(dir):
            if not os.path.exists(dir + '/README.md'):
                return "README.md not found at root!"

            with open(dir + '/README.md') as f:
                first_line = f.readline()

            if not first_line.startswith("# Sample Repo for Tests"):
                return "Concrete README.md must be at root! Instead got: '" + first_line + "'"
            return None

        self.assertRevisionContains(condition=root_contains_concrete_readme)

    def assertRevisionContains(self, condition):
        response = requests.get(LATEST)
        if response.headers["Content-Type"] != "application/zip":
            self.fail(msg='Wrong content received: ' + response.text)
            return

        content_disposition = response.headers["Content-Disposition"]
        file_name = content_disposition[content_disposition.index("=") + 1:]
        output_file = "/tmp/" + file_name
        with open(output_file, "wb") as file:
            file.write(response.content)

        output_dir = "/tmp/" + self._testMethodName
        os.system("rm -rf "+output_dir)
        with zipfile.ZipFile(output_file, 'r') as zip_ref:
            zip_ref.extractall(output_dir)
        dir_contents = subprocess.check_output('tree -a ' + output_dir, universal_newlines=True, shell=True)
        failure = condition(output_dir + "/repo")

        if failure:
            self.fail(failure + "\n" + dir_contents)
        print(dir_contents, flush=True)


if __name__ == '__main__':
    unittest.main()
