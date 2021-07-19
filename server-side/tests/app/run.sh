echo 'Giving backend some time to boot...'
pytest --html=/app/reports/report.html --self-contained-html /app/*.py
