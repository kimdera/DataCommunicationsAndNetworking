# COMP-445
Data Communications &amp; Computer Networks – Fall 2022

the commands for this assignment:
- ```httpc help``` to get general help
- ```httpc help get``` to get help regarding the `GET` request
- `httpc help post` to get help regarding the `POST` request
- `httpc get 'http://httpbin.org/get?course=networking&assignment=1'` for the `GET` request with query parameters
- `httpc get -v 'http://httpbin.org/get?course=networking&assignment=1'` for the `GET` request with verbose option
- `httpc post -h Content-Type:application/json --d '{"Assignment": 1}' http://httpbin.org/post` for the `POST` request with online data
- `httpc -v 'http://httpbin.org/get?course=networking&assignment=1' -o requestResponse.txt` to write the response of your request in a .txt file called `requestResponse`.

