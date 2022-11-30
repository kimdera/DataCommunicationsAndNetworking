# COMP-445
Data Communications &amp; Computer Networks – Fall 2022

Run router from router/source
go run router.go --port=3000 --drop-rate=0.2 --max-delay=10ms --seed=1
--------------------------------------------------------------------------------------------
Run server
httpfs -d /Users/Kim/Desktop/lab3/445-a3/UDP-Console-Server-master/src
httpfs -v -p 8080 -d /Users/Kim/Desktop/lab3/445-a3/UDP-Console-Server-master/src
--------------------------------------------------------------------------------------------
Run client
httpc get -v 'http://localhost:8007'
httpc get -v 'http://localhost:8007/testFile.txt'
httpc post -v -h Content-Type:application/json --d '{"Assignment": 3}' 'http://localhost:8007/testFile.txt'
httpc get -v 'http://localhost:8007/get?course=networking&assignment=3'
httpc post -h Content-Type:application/json --d '{"Assignment": 3}' 'http://localhost:8007/post'



