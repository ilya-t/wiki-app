import base64
import subprocess
import unittest

import requests
import backend
import zipfile
import os

HOST = 'http://test-backend'
LATEST = HOST + '/api/1/revision/latest'
STAGE_API = HOST + '/api/1/stage'
COMMIT_API = HOST + '/api/1/commit'


class ChatAcceptanceTests(unittest.TestCase):
    def setUp(self) -> None:
        super().setUp()
        backend.wait_boot()

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
                return "README.md must start with: '# Sample Repo for Tests'!\nInstead got: '" + first_line + "'"
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

    def test_staging(self):
        expected_contents = "# Sample Repo for Tests\nSTAGED!"
        requests.post(STAGE_API, json={
            'files': [
                {
                    'path': 'README.md',
                    'content': base64.b64encode(expected_contents.encode('utf-8')).decode("utf-8"),
                }
            ]
        })

        actual_contents = subprocess.check_output('cat /app/repo/README.md', universal_newlines=True, shell=True)

        self.assertEqual(expected_contents, actual_contents)

    def test_commitment(self):
        requests.post(STAGE_API, json={
            'files': [
                {
                    'path': 'new_file.md',
                    'content': base64.b64encode("# sample content".encode('utf-8')).decode("utf-8"),
                }
            ]
        })

        response = requests.post(COMMIT_API, json={})

        expected_message = "auto-commit from wiki-app"

        actual_message = subprocess.check_output('cd /app/repo && git log -1', universal_newlines=True, shell=True)

        if expected_message not in actual_message:
            self.fail("'"+expected_message + "' not found in:\n" + actual_message + "\n commit response:\n"+response.text)

if __name__ == '__main__':
    unittest.main()
