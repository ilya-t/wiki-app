import json
import subprocess
import unittest

import api
import backend

HOST = 'http://test-backend'
REPO_NAME = 'test_repo'
REPO_DIR = '/app/repo-store-volume/'+REPO_NAME
BARE_REPO_PATH = '/tmp/'+REPO_NAME+'.git'


class AcceptanceTests(unittest.TestCase):

    def setUp(self) -> None:
        super().setUp()
        self.api_user = api.RestApi(endpoint=HOST, project='test_repo', artifacts_prefix=self.id())
        self.repo_user = api.GitApi(origin=BARE_REPO_PATH, dir="/tmp/repo_user")
        backend.wait_boot()

    def test_project_config(self):
        projects = self.api_user.get_projects()
        for p in projects:
            if p['name'] == REPO_NAME:
                self.assertEqual(BARE_REPO_PATH, p['repo_url'])
                return
            
        self.fail(msg='Could not find '+REPO_NAME+'at: \n'+json.dumps(projects, indent=4))

    def test_latest_revision_zip_content_not_contains_git(self):
        _, files = self.api_user.latest_revision()
        self.assertFalse('.git' in files, msg=json.dumps(files, indent=4))

    def test_sync_skips_up_to_date_files(self):
        _, files = self.api_user.sync(local_state = [
            {
                'name': 'file_with_fixed_sha1',
                'hash': '0758fe8844f102aaa616c30c94ea4f8eb9326b06'
            }
        ])
        self.assertFalse('file_with_fixed_sha1' in files, msg=json.dumps(files, indent=4))

    def test_sync_adds_unspecified_files(self):
        _, files = self.api_user.sync(local_state = [
            {
                'name': 'file_with_fixed_sha1',
                'hash': '0758fe8844f102aaa616c30c94ea4f8eb9326b06'
            }
        ])
        self.assertTrue('README.md' in files, msg=json.dumps(files, indent=4))

    def test_sync_adds_outdated_files(self):
        _, files = self.api_user.sync(local_state = [
            {
                'name': 'file_with_fixed_sha1',
                'hash': '___'
            }
        ])
        self.assertTrue('file_with_fixed_sha1' in files, msg=json.dumps(files, indent=4))

    def test_latest_revision_zip_content_contains_root_content(self):
        _, files = self.api_user.latest_revision()
        self.assertTrue('README.md' in files, msg=json.dumps(files, indent=4))
        readme = files['README.md']
        self.assertEqual('# Sample Repo for Tests\n', readme)

    def test_staging(self):
        expected_contents = '# Sample Repo for Tests\nSTAGED!'
        self.api_user.stage(file='README.md', content=expected_contents)
        actual_contents = subprocess.check_output('cat ' + REPO_DIR + '/README.md', universal_newlines=True, shell=True)

        self.assertEqual(expected_contents, actual_contents)

    def test_commitment(self):
        self.api_user.stage(file='new_file.md', content='# sample content')
        expected_message = 'custom commit message!'
        response = self.api_user.commit(message=expected_message)

        actual_message = subprocess.check_output('cd ' + REPO_DIR + ' && git log -1', universal_newlines=True,
                                                 shell=True)

        if response.status_code != 200:
            self.fail(
                "response returned non-200 code: " + str(response.status_code) + "\n response body:\n" + response.text)

        if expected_message not in actual_message:
            self.fail(
                "'" + expected_message + "' not found in:\n" + actual_message + "\n commit response:\n" + response.text)

    def test_rebase_after_stage(self):
        self.api_user.stage(file='api_file.md', content='content by api')
        self.repo_user.submit(file='test_rebase_after_stage.md', content='# content by git')
        self.api_user.stage(file='api_file.md', content='# content by api')

        _, files = self.api_user.latest_revision()

        self.assertEqual('# content by api', files['api_file.md'], msg=json.dumps(files, indent=4))
        self.assertEqual('# content by git', files['test_rebase_after_stage.md'], msg=json.dumps(files, indent=4))

    def test_rebase_after_commit(self):
        self.api_user.stage(file='api_file.md', content='content by api')
        self.repo_user.submit(file='git_file.md', content='# content by git')
        self.api_user.stage(file='api_file.md', content='# content by api')
        self.api_user.commit(message='content by api commited!')

        _, files = self.api_user.latest_revision()

        self.assertEqual('# content by git', files['git_file.md'], msg=json.dumps(files, indent=4))

    def test_push_after_commit(self):
        self.api_user.stage(file='api_file.md', content='# content by api')
        self.api_user.commit(message='content by api commited!')
        files = self.repo_user.latest_revision()
        self.assertTrue('api_file.md' in files, msg=json.dumps(files, indent=4))
        self.assertEqual('# content by api', files['api_file.md'])

    def test_status_new_file(self):
        file_path = 'status/new_file.md'
        self.api_user.stage(file=file_path, content='# diff by status test')
        status = self.api_user.status()

        files: [dict] = status['files']
        file = find(file_path, files)
        self.assertEqual(file_path, file['path'], msg='raw json: ' + json.dumps(status, indent=4))
        self.assertEqual('new', file['status'], msg='raw json: ' + json.dumps(status, indent=4))
        expected = ''
        self.assertTrue(expected in file['diff'], msg='Expecting "'+expected+'" in diff, got: ' + file['diff'])

    def test_status_modified_file(self):
        file_path = 'status/modified_file.md'
        self.api_user.stage(file=file_path, content='# initial text')
        self.api_user.commit('add modified_file')
        self.api_user.stage(file=file_path, content='# modified text')
        status = self.api_user.status()

        files: [dict] = status['files']
        file = find(file_path, files)
        self.assertEqual(file_path, file['path'], msg='raw json: ' + json.dumps(status, indent=4))
        self.assertEqual('modified', file['status'], msg='raw json: ' + json.dumps(status, indent=4))
        expected = 'modified text'
        self.assertTrue(expected in file['diff'], msg='Expecting "'+expected+'" in diff, got: ' + file['diff'])


def find(path: str, files: [dict]) -> dict:
    for f in files:
        if f['path'] == path:
            return f
    raise Exception('File "'+path+'" not found in ' + str(files))





if __name__ == '__main__':
    unittest.main()
