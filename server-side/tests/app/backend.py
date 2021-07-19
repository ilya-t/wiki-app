import requests
import time

def wait_boot():
    time_to_wait = 2
    timeout = time_to_wait
    while True:
        try:
            response = requests.get('http://test-backend/api/health')
            if response.status_code == 200:
                if time_to_wait != timeout:
                    print("Backend came alive after: " + str(time_to_wait - timeout), flush=True)
                return
            
            if timeout <= 0:
                raise Exception("Server didn't come alive!")
        except:
            timeout -= .5
            time.sleep(0.5)