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

    def __init__(self, endpoint: str, artifacts_prefix: str) -> None:
        super().__init__()
        self._endpoint: str = endpoint
        self._artifacts_prefix: str = artifacts_prefix
        self._stage_api: str = self._endpoint + '/api/1/stage'
        self._commit_api: str = self._endpoint + '/api/1/commit'
        self._latest_api: str = self._endpoint + '/api/1/revision/latest'
        self._sync_api: str = self._endpoint + '/api/1/revision/sync'

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
        tmp_dir = '/tmp/' + self._artifacts_prefix + '_latest_' + str(random.getrandbits(128))
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

    def sync(self, local_state: [dict]) -> (str, {}):
        tmp_dir = '/tmp/' + self._artifacts_prefix + '_sync_' + str(random.getrandbits(128))
        os.makedirs(tmp_dir)

        response = requests.post(self._sync_api, json=local_state)
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
        if os.path.exists(self._repo_dir+'/.git'):
            return
        self._run_cmd('mkdir -p ' + self._repo_dir, working_dir='/')
        cmd = 'git clone ' + self._origin + ' ' + self._repo_dir
        out = self._run_cmd(cmd)
        print('Cloning...', out)

    def _make_commit(self, file: str, content: str):
        commit_file = self._repo_dir + '/' + file
        self._run_cmd('echo -n "'+content+'" > ' + commit_file)
        self._run_cmd('cat '+commit_file)
        self._run_cmd('git add ' + file)
        self._run_cmd('git config --local user.email "another@contributor"')
        self._run_cmd('git config --local user.name "Another Contributor"')
        self._run_cmd('git status')
        self._run_cmd('git commit --message="added ' + file + '"')


    def _push(self):
        self._run_cmd('git push origin master')
        pass

    def _pull(self):
        self._run_cmd('git pull origin master')
        pass

    def latest_revision(self) -> {}:
        self._try_clone()
        self._pull()
        return scan_dir_relative(self._repo_dir)

    def _run_cmd(self, command: str, working_dir: str = None):
        if not working_dir:
            working_dir = self._repo_dir
        with subprocess.Popen(command,
                              shell=True,
                              stdout=subprocess.PIPE,
                              stderr=subprocess.PIPE,
                              universal_newlines=True,
                              cwd=working_dir) as p:
            result = p.wait()
            output = p.stdout.readlines()
            error = p.stderr.readlines()

            print('cmd: "', command, '":')
            for line in output:
                print(line)
            if result != 0:
                print("error output:")
                for line in error:
                    print(line)

        if result != 0:
            raise Exception('shell command failed: ' + command + 
                '\n with error output: ' + ''.join(error) + 
                '\nstdout:'+''.join(output))
        else:
            return result