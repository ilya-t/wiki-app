import unittest

import requests
import backend

HOST = 'http://test-backend'
LATEST = HOST + '/api/1/get_latest'
INITIAL_COMMIT_HASH = '85f439fffceaf58f612374522b0b66d508b9a2e7'

class ChatAcceptanceTests(unittest.TestCase):
    def setUp(self) -> None:
        super().setUp()
        backend.wait_boot()

    def test_latest_revision(self):
        response = requests.get(LATEST)

        print(response.json(), flush=True)
        self.assertEqual(INITIAL_COMMIT_HASH, response.json()['revision'],
                         msg='Expected same payload, but got: ' + str(response.json()))

if __name__ == '__main__':
    unittest.main()
