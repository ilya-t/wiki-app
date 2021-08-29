import base64
import os
import subprocess
import zipfile
import random

import requests

DIR_TAG = '<DIR>'


def scan_dir_relative(directory: str) -> {}:
    abs_path_scan = scan_dir(directory)
    relative_path_scan = {}
    slash = 1
    for abs_path in abs_path_scan:
        relative_path = abs_path[len(directory) + slash:]
        relative_path_scan[relative_path] = abs_path_scan[abs_path]
    return relative_path_scan


def scan_dir(directory: str) -> {}:
    results: dict[str, str] = {}
    for element in os.listdir(directory):
        element_path = directory + '/' + element
        if os.path.isdir(element_path):
            results[element_path] = DIR_TAG
            results.update(scan_dir(element_path))
        else:
            with open(element_path, 'r') as f:
                try:
                    content = f.read()
                except Exception as e:
                    content = str(e)

                results[element_path] = content

    return results


class RestApi:

    def __init__(self, endpoint: str) -> None:
        super().__init__()
        self._endpoint: str = endpoint
        self._stage_api: str = self._endpoint + '/api/1/stage'
        self._commit_api: str = self._endpoint + '/api/1/commit'
        self._latest_api: str = self._endpoint + '/api/1/revision/latest'

    def stage(self, file: str, content: str) -> requests.Response:
        return requests.post(self._stage_api, json={
            'files': [
                {
                    'path': file,
                    'content': base64.b64encode(content.encode('utf-8')).decode("utf-8"),
                }
            ]
        })

    def commit(self, message: str) -> requests.Response:
        return requests.post(self._commit_api, json={'message': message})

    def latest_revision(self) -> (str, {}):
        tmp_dir = '/tmp/' + str(random.getrandbits(128))
        os.makedirs(tmp_dir)

        response = requests.get(self._latest_api)
        if response.headers["Content-Type"] != "application/zip":
            raise Exception('Wrong content received: ' + response.text)

        content_disposition = response.headers["Content-Disposition"]
        file_name = content_disposition[content_disposition.index("=") + 1:]
        output_file = tmp_dir + '/' + file_name

        with open(output_file, "wb") as file:
            file.write(response.content)

        output_dir = tmp_dir + '/extracted'
        os.system("rm -rf " + output_dir)
        with zipfile.ZipFile(output_file, 'r') as zip_ref:
            zip_ref.extractall(output_dir)
        repo_dir = output_dir + '/repo'
        return file_name, scan_dir_relative(repo_dir)


class GitApi:
    def __init__(self, origin: str, dir: str) -> None:
        super().__init__()
        self._origin = origin
        self._repo_dir = dir

    def submit(self, file: str, content: str):
        self._try_clone()
        self._make_commit(file, content)
        self._push()

    def _try_clone(self):
        if os.path.exists(self._repo_dir):
            return
        cmd = 'git clone ' + self._origin + ' ' + self._repo_dir
        out = subprocess.check_output(cmd, universal_newlines=True, shell=True)
        print('Cloning...', out)

    def _make_commit(self, file: str, content: str):
        commit_file = self._repo_dir + '/' + file
        with open(commit_file, "w") as f:
            f.write(content)

        cmd = ' && '.join([
            'cd ' + self._repo_dir,
            'git add ' + file,
            'git config --local user.email "another@contributor"',
            'git config --local user.name "Another Contributor"',
            'git commit --message="added ' + file + '"'
        ])

        out = subprocess.check_output(cmd, universal_newlines=True, shell=True)
        print('Committed:', out)

    def _push(self):
        cmd = 'cd ' + self._repo_dir + ' && git push origin master'
        out = subprocess.check_output(cmd, universal_newlines=True, shell=True)
        print('Pushed:', out)
        pass

    def _pull(self):
        cmd = 'cd ' + self._repo_dir + ' && git pull origin master'
        out = subprocess.check_output(cmd, universal_newlines=True, shell=True)
        print('Pulled:', out)
        pass

    def latest_revision(self) -> {}:
        self._try_clone()
        self._pull()
        return scan_dir_relative(self._repo_dir)
